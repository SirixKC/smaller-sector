package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityListener;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * Intercepts derelict ship discovery and replaces cruisers/capitals with smaller ships.
 *
 * This listener is called when the player discovers a derelict entity.
 * We modify the ship data BEFORE the player sees what type of ship it is.
 */
public class DerelictInterceptor implements DiscoverEntityListener {

    private static final Logger log = Global.getLogger(DerelictInterceptor.class);
    private static final Random random = new Random();
    private static final String PROCESSED_TAG = "smallersector_derelict_processed";

    @Override
    public void reportEntityDiscovered(SectorEntityToken entity) {
        if (entity == null) return;

        // Check if this entity has a derelict ship plugin
        if (!(entity.getCustomPlugin() instanceof DerelictShipEntityPlugin)) {
            return;
        }

        DerelictShipEntityPlugin plugin = (DerelictShipEntityPlugin) entity.getCustomPlugin();
        DerelictShipData data = plugin.getData();

        if (data == null || data.ship == null) {
            return;
        }

        // Skip if already processed (prevents re-rolling on game load)
        if (entity.hasTag(PROCESSED_TAG)) {
            return;
        }

        processDerelictShip(entity, data);
    }

    private void processDerelictShip(SectorEntityToken entity, DerelictShipData data) {
        PerShipData shipData = data.ship;

        // Get the variant to determine hull size
        // PerShipData.getVariant() handles loading from variantId if needed
        ShipVariantAPI variant = shipData.getVariant();
        if (variant == null) {
            log.debug("DerelictInterceptor: Could not get variant for " + shipData.variantId);
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

        // Determine faction for role matching (use entity faction or default to independent)
        String factionId = "independent";
        if (entity.getFaction() != null) {
            factionId = entity.getFaction().getId();
        }

        // Check blacklist
        if (Settings.isFactionBlacklisted(factionId)) {
            return;
        }

        // Try to replace based on probability settings
        String replacementVariantId = tryGetReplacement(variant, factionId, size);

        if (replacementVariantId != null) {
            log.info("Smaller Sector: Replacing derelict " + shipData.variantId +
                     " with " + replacementVariantId);
            shipData.variantId = replacementVariantId;
            shipData.variant = null; // Clear cached variant so it reloads
        }

        // Mark as processed
        entity.addTag(PROCESSED_TAG);
    }

    private String tryGetReplacement(ShipVariantAPI original, String factionId, HullSize size) {
        int roll = random.nextInt(100);
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

        // Try to find a valid variant for the replacement hull
        String hullId = replacement.getHullId();

        // Try standard patterns in order of preference
        String[] patterns = {
            hullId + "_Hull",           // Standard hull-only variant
            hullId + "_standard",       // Some mods use this
            hullId + "_Standard",       // Case variation
            hullId                      // Just the hull ID (sometimes works)
        };

        for (String variantId : patterns) {
            ShipVariantAPI testVariant = Global.getSettings().getVariant(variantId);
            if (testVariant != null) {
                return variantId;
            }
        }

        // If no variant found, try to get any variant for this hull
        java.util.List<String> allVariants = Global.getSettings().getHullIdToVariantListMap().get(hullId);
        if (allVariants != null && !allVariants.isEmpty()) {
            return allVariants.get(0);
        }

        log.warn("DerelictInterceptor: Could not find valid variant for hull " + hullId);
        return null;
    }
}
