package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ShipReplacer {

    private static final Random RANDOM = new Random();

    private static final String[] FACTION_VARIANT_ROLES = {
        ShipRoles.MARKET_RANDOM_SHIPS,
        ShipRoles.COMBAT_SMALL_FOR_SMALL_FLEET,
        ShipRoles.COMBAT_SMALL,
        ShipRoles.COMBAT_MEDIUM,
        ShipRoles.COMBAT_LARGE,
        ShipRoles.COMBAT_CAPITAL,
        ShipRoles.COMBAT_FREIGHTER_SMALL,
        ShipRoles.COMBAT_FREIGHTER_MEDIUM,
        ShipRoles.COMBAT_FREIGHTER_LARGE,
        ShipRoles.CIV_RANDOM,
        ShipRoles.PHASE_SMALL,
        ShipRoles.PHASE_MEDIUM,
        ShipRoles.PHASE_LARGE,
        ShipRoles.PHASE_CAPITAL,
        ShipRoles.CARRIER_SMALL,
        ShipRoles.CARRIER_MEDIUM,
        ShipRoles.CARRIER_LARGE,
        ShipRoles.FREIGHTER_SMALL,
        ShipRoles.FREIGHTER_MEDIUM,
        ShipRoles.FREIGHTER_LARGE,
        ShipRoles.TANKER_SMALL,
        ShipRoles.TANKER_MEDIUM,
        ShipRoles.TANKER_LARGE,
        ShipRoles.PERSONNEL_SMALL,
        ShipRoles.PERSONNEL_MEDIUM,
        ShipRoles.PERSONNEL_LARGE,
        ShipRoles.LINER_SMALL,
        ShipRoles.LINER_MEDIUM,
        ShipRoles.LINER_LARGE,
        ShipRoles.TUG,
        ShipRoles.CRIG,
        ShipRoles.UTILITY,
        ShipRoles.FAST_ATTACK,
        ShipRoles.ESCORT_SMALL,
        ShipRoles.ESCORT_MEDIUM
    };

    /**
     * Attempts to replace a fleet member with a smaller ship.
     * Returns null if ship should not be replaced.
     */
    public static FleetMemberAPI tryReplace(FleetMemberAPI original, String factionId) {
        return tryReplaceForFleet(original, factionId);
    }

    /** Attempt a replacement that must have a usable NPC combat loadout. */
    public static FleetMemberAPI tryReplaceForFleet(FleetMemberAPI original, String factionId) {
        return tryReplace(original, factionId, true);
    }

    /** Attempt a replacement represented as a mothballed market-sale hull. */
    public static FleetMemberAPI tryReplaceForMarket(FleetMemberAPI original, String factionId) {
        return tryReplace(original, factionId, false);
    }

    private static FleetMemberAPI tryReplace(
            FleetMemberAPI original, String factionId, boolean requireOutfitted) {
        if (original == null || original.getHullSpec() == null) {
            return null;
        }

        // Stations and story/unique ships are part of the campaign contract,
        // not procedural replacement candidates.
        if (isProtectedShip(original)) {
            return null;
        }

        // Check faction blacklist
        if (Settings.isFactionBlacklisted(factionId)) {
            return null;
        }

        HullSize size = original.getHullSpec().getHullSize();

        if (size == HullSize.CRUISER) {
            return tryReplaceCruiser(original, factionId, requireOutfitted);
        } else if (size == HullSize.CAPITAL_SHIP) {
            return tryReplaceCapital(original, factionId, requireOutfitted);
        }

        return null;
    }

    /**
     * Returns true when replacing this member could break a station, mission,
     * unique-ship reference, or explicitly non-recoverable encounter.
     */
    public static boolean isProtectedShip(FleetMemberAPI member) {
        if (member == null) return true;
        if (member.isStation()) return true;
        return isProtectedVariant(member.getVariant()) || isProtectedHull(member.getHullSpec());
    }

    /**
     * Variant-level story protection shared by fleet, market, derelict, and
     * player-acquisition paths.
     */
    public static boolean isProtectedVariant(ShipVariantAPI variant) {
        if (variant == null) return false;
        return variant.hasTag(Tags.STORY_CRITICAL)
                || variant.hasTag(Tags.SHIP_UNIQUE_SIGNATURE)
                || variant.hasTag(Tags.MISSION_ITEM)
                || variant.hasTag(Tags.VARIANT_UNBOARDABLE)
                || variant.hasTag(Tags.UNRECOVERABLE)
                || variant.hasTag(Tags.VARIANT_FX_DRONE)
                || variant.hasTag(Tags.MONSTER)
                || variant.hasTag(Tags.NO_MARKET_INFO)
                || variant.hasTag(Tags.NO_SELL);
    }

    public static boolean isProtectedHull(ShipHullSpecAPI hull) {
        if (hull == null) return false;
        return hull.hasTag(Tags.STORY_CRITICAL)
                || hull.hasTag(Tags.SHIP_UNIQUE_SIGNATURE)
                || hull.hasTag(Tags.MISSION_ITEM)
                || hull.hasTag(Tags.VARIANT_UNBOARDABLE)
                || hull.hasTag(Tags.UNRECOVERABLE)
                || hull.hasTag(Tags.VARIANT_FX_DRONE)
                || hull.hasTag(Tags.MONSTER)
                || hull.hasTag(Tags.NO_MARKET_INFO)
                || hull.hasTag(Tags.NO_SELL);
    }

    private static FleetMemberAPI tryReplaceCruiser(
            FleetMemberAPI original, String factionId, boolean requireOutfitted) {
        int roll = RANDOM.nextInt(100);

        int frigateChance = Settings.getCruiserToFrigate();
        int destroyerChance = Settings.getCruiserToDestroyer();

        HullSize targetSize;
        if (roll < frigateChance) {
            targetSize = HullSize.FRIGATE;
        } else if (roll < frigateChance + destroyerChance) {
            targetSize = HullSize.DESTROYER;
        } else {
            return null; // Stays cruiser
        }

        return createReplacement(original, factionId, targetSize, requireOutfitted);
    }

    private static FleetMemberAPI tryReplaceCapital(
            FleetMemberAPI original, String factionId, boolean requireOutfitted) {
        int roll = RANDOM.nextInt(100);

        int frigateChance = Settings.getCapitalToFrigate();
        int destroyerChance = Settings.getCapitalToDestroyer();
        int cruiserChance = Settings.getCapitalToCruiser();

        HullSize targetSize;
        if (roll < frigateChance) {
            targetSize = HullSize.FRIGATE;
        } else if (roll < frigateChance + destroyerChance) {
            targetSize = HullSize.DESTROYER;
        } else if (roll < frigateChance + destroyerChance + cruiserChance) {
            targetSize = HullSize.CRUISER;
            // Note: Capitals becoming cruisers are NOT re-checked
        } else {
            return null; // Stays capital
        }

        return createReplacement(original, factionId, targetSize, requireOutfitted);
    }

    private static FleetMemberAPI createReplacement(
            FleetMemberAPI original,
            String factionId,
            HullSize targetSize,
            boolean requireOutfitted) {

        if (original == null || original.getHullSpec() == null) {
            return null;
        }

        ShipHullSpecAPI replacement = RoleMatcher.findReplacement(
            original.getHullSpec(),
            factionId,
            targetSize
        );

        if (replacement == null) {
            return null; // No valid replacement, keep original
        }

        String variantId = requireOutfitted
                ? findOutfittedVariantId(replacement, factionId)
                : findEmptyHullVariantId(replacement);
        if (variantId == null) {
            return null;
        }

        FleetMemberAPI newMember = Global.getFactory().createFleetMember(
            FleetMemberType.SHIP,
            variantId
        );
        if (newMember == null) return null;

        copyMemberState(original, newMember);
        return newMember;
    }

    /** Pick an outfitted stock variant that belongs to the replacement faction. */
    public static String findOutfittedVariantId(ShipHullSpecAPI hull, String factionId) {
        if (hull == null || Global.getSettings() == null) return null;

        Set<String> factionCandidates = new LinkedHashSet<String>();
        if (Global.getSector() != null && factionId != null) {
            FactionAPI faction = Global.getSector().getFaction(factionId);
            if (faction != null) {
                factionCandidates.addAll(getFactionVariantIds(faction));
            }
        }

        String picked = pickValidVariant(factionCandidates, hull, true);
        if (picked != null) return picked;

        List<String> allHullVariants = Global.getSettings()
                .getHullIdToVariantListMap().get(hull.getHullId());
        return pickValidVariant(allHullVariants, hull, true);
    }

    /**
     * Infer the faction whose role tables contain a neutral derelict's variant.
     * Exact variant ownership wins; known-hull ownership is the fallback.
     */
    public static String findOwningFactionId(ShipVariantAPI variant) {
        if (variant == null || variant.getHullSpec() == null || Global.getSector() == null) {
            return "independent";
        }

        List<FactionAPI> hullOwners = new ArrayList<FactionAPI>();
        List<FactionAPI> exactOwners = new ArrayList<FactionAPI>();
        String hullId = variant.getHullSpec().getHullId();
        String variantId = variant.getHullVariantId();

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (faction == null || faction.isNeutralFaction() || faction.isPlayerFaction()) continue;
            if (faction.getKnownShips() == null || !faction.getKnownShips().contains(hullId)) continue;

            hullOwners.add(faction);
            if (variantId != null && getFactionVariantIds(faction).contains(variantId)) {
                exactOwners.add(faction);
            }
        }

        FactionAPI owner = pickPreferredOwner(exactOwners);
        if (owner == null) owner = pickPreferredOwner(hullOwners);
        return owner != null ? owner.getId() : "independent";
    }

    /** Pick a validated empty-hull variant for vanilla-style market inventory. */
    public static String findEmptyHullVariantId(ShipHullSpecAPI hull) {
        if (hull == null || Global.getSettings() == null) return null;

        List<String> candidates = new ArrayList<String>();
        candidates.add(hull.getHullId() + "_Hull");
        List<String> allHullVariants = Global.getSettings()
                .getHullIdToVariantListMap().get(hull.getHullId());
        if (allHullVariants != null) {
            candidates.addAll(allHullVariants);
        }

        return pickValidVariant(candidates, hull, false);
    }

    private static String pickValidVariant(
            Iterable<String> variantIds, ShipHullSpecAPI targetHull, boolean requireOutfitted) {
        if (variantIds == null || targetHull == null) return null;

        List<String> valid = new ArrayList<String>();
        for (String variantId : variantIds) {
            if (variantId == null || !Global.getSettings().doesVariantExist(variantId)) continue;

            ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
            if (variant == null || variant.getHullSpec() == null) continue;
            if (!targetHull.getHullId().equals(variant.getHullSpec().getHullId())) continue;
            if (variant.isStation() || variant.isFighter()) continue;
            if (isProtectedVariant(variant) || isProtectedHull(variant.getHullSpec())) continue;
            if (requireOutfitted == variant.isEmptyHullVariant()) continue;

            valid.add(variantId);
        }

        if (valid.isEmpty()) return null;
        return valid.get(RANDOM.nextInt(valid.size()));
    }

    private static Set<String> getFactionVariantIds(FactionAPI faction) {
        Set<String> variants = new LinkedHashSet<String>();
        if (faction == null) return variants;

        for (String role : FACTION_VARIANT_ROLES) {
            Set<String> roleVariants = faction.getVariantsForRole(role);
            if (roleVariants != null) {
                variants.addAll(roleVariants);
            }
        }

        Set<String> restricted = faction.getRestrictToVariants();
        if (restricted != null) {
            variants.addAll(restricted);
        }

        Map<String, Float> overrides = faction.getVariantOverrides();
        if (overrides != null) {
            variants.addAll(overrides.keySet());
        }
        return variants;
    }

    private static FactionAPI pickPreferredOwner(List<FactionAPI> owners) {
        for (FactionAPI owner : owners) {
            if (Settings.isFactionBlacklisted(owner.getId())) return owner;
        }
        return owners.isEmpty() ? null : owners.get(0);
    }

    private static void copyMemberState(FleetMemberAPI original, FleetMemberAPI replacement) {
        if (original.getId() != null) replacement.setId(original.getId());
        if (original.getCaptain() != null) replacement.setCaptain(original.getCaptain());
        if (original.getShipName() != null) replacement.setShipName(original.getShipName());

        replacement.setOwner(original.getOwner());
        replacement.setAlly(original.isAlly());
        replacement.setPersonalityOverride(original.getPersonalityOverride());

        RepairTrackerAPI oldTracker = original.getRepairTracker();
        RepairTrackerAPI newTracker = replacement.getRepairTracker();
        if (oldTracker != null && newTracker != null) {
            newTracker.setMothballed(oldTracker.isMothballed());
            newTracker.setCrashMothballed(oldTracker.isCrashMothballed());
            newTracker.setSuspendRepairs(oldTracker.isSuspendRepairs());
            newTracker.setCRPriorToMothballing(oldTracker.getCRPriorToMothballing());
            newTracker.setCR(Math.min(oldTracker.getCR(), newTracker.getMaxCR()));
        }

        if (original.getStatus() != null && replacement.getStatus() != null) {
            replacement.getStatus().setHullFraction(original.getStatus().getHullFraction());
        }

        replacement.setStatUpdateNeeded(true);
    }

}
