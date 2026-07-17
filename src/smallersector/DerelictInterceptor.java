package smallersector;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityListener;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import org.apache.log4j.Logger;
import org.magiclib.ReflectionUtils;

import java.util.Random;

/**
 * Replaces eligible procedural derelicts before the player sees their ship type.
 * Discovery events are supplemented by a bounded current-location scan because
 * vanilla wreck entities are not discoverable by default.
 */
public class DerelictInterceptor implements DiscoverEntityListener, EveryFrameScript {

    private static final Logger log = Global.getLogger(DerelictInterceptor.class);
    private static final Random RANDOM = new Random();
    private static final String PROCESSED_TAG = "smallersector_derelict_processed";
    private static final String MIGRATION_KEY =
            "smallersector_derelict_processing_initialized_v1";
    // Vanilla story wrecks that predate consistent mission-important tagging.
    private static final String GATE_SCAN_DERELICT_KEY = "$gateScanDerelict";
    private static final String ANCIENT_ONSLAUGHT_KEY = "$onslaughtMkI";

    /**
     * Establish the derelict baseline before campaign control is returned.
     * Existing-save entities are grandfathered once; new-game and subsequently
     * missed entities are processed immediately, before they can be rendered.
     */
    public int initializeExistingEntities(boolean newGame) {
        if (Global.getSector() == null) return 0;

        boolean migrationComplete = Boolean.TRUE.equals(
                Global.getSector().getPersistentData().get(MIGRATION_KEY));
        boolean grandfather = !newGame && !migrationComplete;
        int count = 0;

        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CustomCampaignEntityAPI entity : location.getCustomEntities()) {
                if (entity == null || entity.hasTag(PROCESSED_TAG)) continue;
                if (!(entity.getCustomPlugin() instanceof DerelictShipEntityPlugin)) continue;

                if (grandfather) {
                    entity.addTag(PROCESSED_TAG);
                } else {
                    reportEntityDiscovered(entity);
                }
                count += 1;
            }
        }

        Global.getSector().getPersistentData().put(MIGRATION_KEY, Boolean.TRUE);
        return count;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null || Global.getSector().getCurrentLocation() == null) return;

        // DerelictShipEntityPlugin sets discoverable=false by default, so the
        // discovery event alone cannot cover dynamically created wrecks. A scan
        // of only the current location runs before campaign rendering and catches
        // new entities after their creating call has attached story protection.
        for (CustomCampaignEntityAPI entity :
                Global.getSector().getCurrentLocation().getCustomEntities()) {
            if (entity == null || entity.hasTag(PROCESSED_TAG)) continue;
            if (entity.getCustomPlugin() instanceof DerelictShipEntityPlugin) {
                reportEntityDiscovered(entity);
            }
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void reportEntityDiscovered(SectorEntityToken entity) {
        if (entity == null) return;

        // Check if this entity has a derelict ship plugin
        if (!(entity.getCustomPlugin() instanceof DerelictShipEntityPlugin)) {
            return;
        }

        // Discovery is a one-shot decision. Mark before any data lookup so an
        // unchanged, protected, or malformed derelict is stable across reloads.
        if (entity.hasTag(PROCESSED_TAG)) return;
        entity.addTag(PROCESSED_TAG);

        if (isProtectedEntity(entity)) {
            return;
        }

        DerelictShipEntityPlugin plugin = (DerelictShipEntityPlugin) entity.getCustomPlugin();
        DerelictShipData data = plugin.getData();

        if (data == null || data.ship == null) {
            return;
        }

        processDerelictShip(entity, data);
    }

    private void processDerelictShip(SectorEntityToken entity, DerelictShipData data) {
        PerShipData shipData = data.ship;

        // Captained, identity-bearing, always-known, and direct custom variants
        // are strong indicators of scripted content even when a mod omitted the
        // canonical mission tags. Ordinary procgen wrecks also receive random
        // ship names, so shipName alone is intentionally not an exclusion.
        if (isProtectedShipData(shipData)) {
            return;
        }

        // Get the variant to determine hull size
        // PerShipData.getVariant() handles loading from variantId if needed
        ShipVariantAPI variant;
        try {
            variant = shipData.getVariant();
        } catch (Throwable t) {
            log.warn("Smaller Sector: Could not load derelict variant " +
                    shipData.variantId + "; leaving the wreck unchanged.", t);
            return;
        }
        if (variant == null) {
            log.debug("Smaller Sector: Could not get derelict variant for " + shipData.variantId);
            return;
        }

        if (ShipReplacer.isProtectedVariant(variant)) {
            return;
        }

        HullSize size = variant.getHullSize();
        if (variant.getHullSpec() != null && variant.getHullSpec().getHints() != null
                && variant.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)) {
            return; // Never process stations
        }
        if (size != HullSize.CRUISER && size != HullSize.CAPITAL_SHIP) {
            return; // Only process cruisers and capitals
        }

        // Procgen wrecks use the neutral entity faction, which has no ship pool.
        // Independent is the vanilla mixed-faction fallback for such derelicts.
        String factionId = "independent";
        if (entity.getFaction() != null && !entity.getFaction().isNeutralFaction()) {
            factionId = entity.getFaction().getId();
        } else {
            factionId = ShipReplacer.findOwningFactionId(variant);
        }

        // Check blacklist
        if (Settings.isFactionBlacklisted(factionId)) {
            return;
        }

        // Try to replace based on probability settings
        String replacementVariantId = tryGetReplacement(variant, factionId, size);

        if (replacementVariantId != null) {
            String originalVariantId = shipData.variantId;
            ShipVariantAPI originalVariant = shipData.variant;

            shipData.variantId = replacementVariantId;
            shipData.variant = null; // Clear cached variant so it reloads

            // DerelictShipEntityPlugin caches its member and campaign sprite.
            // Refresh that cache now or the old hull remains visible until reload.
            if (refreshDerelictPlugin(entity, (DerelictShipEntityPlugin) entity.getCustomPlugin())) {
                log.debug("Smaller Sector: Replaced derelict " + originalVariantId +
                          " with " + replacementVariantId);
            } else {
                shipData.variantId = originalVariantId;
                shipData.variant = originalVariant;
                // If cache rebuilding failed partway through, make a best-effort
                // pass to restore the original visual and cached member too.
                refreshDerelictPlugin(entity, (DerelictShipEntityPlugin) entity.getCustomPlugin());
            }
        }

    }

    private boolean isProtectedEntity(SectorEntityToken entity) {
        if (entity.hasTag(Tags.STORY_CRITICAL)
                || entity.hasTag(Tags.MISSION_ITEM)
                || entity.hasTag(Tags.MISSION_LOCATION)
                || entity.hasTag(Tags.NOT_RANDOM_MISSION_TARGET)
                || entity.hasTag(Tags.SHIP_UNIQUE_SIGNATURE)) {
            return true;
        }
        if (entity.getMemoryWithoutUpdate() == null) return false;
        return entity.getMemoryWithoutUpdate().contains(MemFlags.STORY_CRITICAL)
                || entity.getMemoryWithoutUpdate().contains(MemFlags.ENTITY_MISSION_IMPORTANT)
                || entity.getMemoryWithoutUpdate().contains(MemFlags.MEMORY_KEY_MISSION_IMPORTANT)
                || entity.getMemoryWithoutUpdate().contains(GATE_SCAN_DERELICT_KEY)
                || entity.getMemoryWithoutUpdate().contains(ANCIENT_ONSLAUGHT_KEY);
    }

    private boolean isProtectedShipData(PerShipData shipData) {
        if (shipData == null) return true;
        if (shipData.captain != null) return true;
        if (shipData.fleetMemberId != null && !shipData.fleetMemberId.trim().isEmpty()) return true;
        if (Boolean.TRUE.equals(shipData.nameAlwaysKnown)) return true;
        return shipData.variant != null && shipData.variantId == null;
    }

    private String tryGetReplacement(ShipVariantAPI original, String factionId, HullSize size) {
        int roll = RANDOM.nextInt(100);
        HullSize targetSize = null;

        if (size == HullSize.CRUISER) {
            int frigateChance = Settings.getCruiserToFrigate();
            int destroyerChance = Settings.getCruiserToDestroyer();

            if (roll < frigateChance) {
                targetSize = HullSize.FRIGATE;
            } else if (roll < frigateChance + destroyerChance) {
                targetSize = HullSize.DESTROYER;
            }
        } else if (size == HullSize.CAPITAL_SHIP) {
            int frigateChance = Settings.getCapitalToFrigate();
            int destroyerChance = Settings.getCapitalToDestroyer();
            int cruiserChance = Settings.getCapitalToCruiser();

            if (roll < frigateChance) {
                targetSize = HullSize.FRIGATE;
            } else if (roll < frigateChance + destroyerChance) {
                targetSize = HullSize.DESTROYER;
            } else if (roll < frigateChance + destroyerChance + cruiserChance) {
                targetSize = HullSize.CRUISER;
            }
        }

        if (targetSize == null) {
            return null; // Ship stays as-is
        }

        // Find replacement using RoleMatcher
        ShipHullSpecAPI originalHull = original.getHullSpec();
        ShipHullSpecAPI replacement = RoleMatcher.findReplacement(originalHull, factionId, targetSize);

        if (replacement == null) {
            return null;
        }

        return ShipReplacer.findOutfittedVariantId(replacement, factionId);
    }

    private boolean refreshDerelictPlugin(
            SectorEntityToken entity, DerelictShipEntityPlugin plugin) {
        try {
            ReflectionUtils.INSTANCE.invoke(
                    "readResolve", plugin, new Object[0], true);

            ShipVariantAPI refreshed = plugin.getData().ship.getVariant();
            if (refreshed == null || refreshed.getHullSpec() == null) return false;

            HullSize size = refreshed.getHullSpec().getHullSize();
            entity.getDetectedRangeMod().modifyFlat(
                    "gen", DerelictShipEntityPlugin.getDetectedAtRange(size));
            if (entity instanceof CustomCampaignEntityAPI) {
                ((CustomCampaignEntityAPI) entity).setRadius(
                        DerelictShipEntityPlugin.getRadius(size));
            }
            return true;
        } catch (Throwable t) {
            log.error("Smaller Sector: Could not refresh replaced derelict visual; " +
                    "keeping the original ship.", t);
            return false;
        }
    }
}
