package smallersector;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.FleetStubAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import java.util.ArrayList;
import java.util.List;

public class FleetInterceptor implements FleetEventListener {

    /** Stored in fleet memory without an expiry, so it survives saving and loading. */
    private static final String PROCESSED_KEY = "$smallersector_fleet_processed";

    // These are widely-used campaign conventions but do not have API constants.
    private static final String DEFENDER_FLEET_KEY = "$defenderFleet";
    private static final String STORY_FLEET_KEY = "$storyFleet";
    private static final String MISSION_FLEET_KEY = "$missionFleet";

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet,
            FleetDespawnReason reason, Object param) {
        // Not needed
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet,
            CampaignFleetAPI primaryWinner, BattleAPI battle) {
        // Not needed
    }

    /**
     * Process a newly spawned fleet, replacing cruisers/capitals at most once.
     *
     * The marker is written before any roll is made. This is intentional: a fleet
     * that rolls "no replacement", belongs to a blacklisted faction, or is
     * protected must not be reconsidered after a save/load or a settings change.
     */
    public static void processFleet(CampaignFleetAPI fleet) {
        if (fleet == null || hasBeenProcessed(fleet)) return;

        markProcessed(fleet);

        if (isProtectedFleet(fleet)) return;
        if (fleet.getFaction() == null || fleet.getFleetData() == null) return;

        String factionId = fleet.getFaction().getId();
        List<FleetMemberAPI> toRemove = new ArrayList<FleetMemberAPI>();
        List<FleetMemberAPI> toAdd = new ArrayList<FleetMemberAPI>();
        List<FleetMemberAPI> replacementFlagships = new ArrayList<FleetMemberAPI>();

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            FleetMemberAPI replacement = ShipReplacer.tryReplaceForFleet(member, factionId);
            if (replacement != null) {
                toRemove.add(member);
                toAdd.add(replacement);
                if (member.isFlagship()) {
                    replacementFlagships.add(replacement);
                }
            }
        }

        // Apply changes
        for (FleetMemberAPI member : toRemove) {
            fleet.getFleetData().removeFleetMember(member);
        }
        for (FleetMemberAPI member : toAdd) {
            fleet.getFleetData().addFleetMember(member);
        }

        if (!toRemove.isEmpty()) {
            // A deflated procgen fleet's own inflater is the safest source of
            // faction weapons, hullmods, quality, S-mods, and D-mods. Running it
            // after insertion ensures replacement members participate too.
            fleet.inflateIfNeeded();

            // addFleetMember() clears this flag. Restore it only after every
            // replacement has been inserted and inflated, without changing captain.
            for (FleetMemberAPI replacement : replacementFlagships) {
                replacement.setFlagship(true, false);
            }

            fleet.getFleetData().sort();
            fleet.updateCounts();
            fleet.updateFleetView();
        }
    }

    /**
     * Mark an already-existing fleet as the migration baseline without altering it.
     */
    public static void grandfatherFleet(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.isPlayerFleet() || hasBeenProcessed(fleet)) return;
        markProcessed(fleet);
    }

    /**
     * Grandfather a deflated fleet stub from an existing save. Starsector carries
     * the stub memory into the fleet when it materializes.
     */
    public static void grandfatherFleetStub(FleetStubAPI stub) {
        if (stub == null) return;
        MemoryAPI memory = stub.getMemoryWithoutUpdate();
        if (memory != null && !memory.contains(PROCESSED_KEY)) {
            memory.set(PROCESSED_KEY, true);
        }
    }

    public static boolean hasBeenProcessed(CampaignFleetAPI fleet) {
        if (fleet == null) return true;
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        return memory != null && memory.contains(PROCESSED_KEY);
    }

    private static void markProcessed(CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        if (memory != null) {
            memory.set(PROCESSED_KEY, true);
        }
    }

    /**
     * Single canonical protection check used by every fleet-processing path.
     */
    public static boolean isProtectedFleet(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.isPlayerFleet()) return true;
        if (fleet.isStationMode() || fleet.hasTag(Tags.STATION)) return true;
        if (fleet.hasScriptOfClass(MissionFleetAutoDespawn.class)) return true;

        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        if (hasAnyMemoryKey(memory,
                MemFlags.STORY_CRITICAL,
                MemFlags.ENTITY_MISSION_IMPORTANT,
                MemFlags.MEMORY_KEY_MISSION_IMPORTANT,
                MemFlags.STATION_FLEET,
                MemFlags.STATION_BASE_FLEET,
                DEFENDER_FLEET_KEY,
                STORY_FLEET_KEY,
                MISSION_FLEET_KEY)) {
            return true;
        }

        if (fleet.hasTag(Tags.STORY_CRITICAL)
                || fleet.hasTag(Tags.MISSION_ITEM)
                || fleet.hasTag(Tags.MISSION_LOCATION)
                || fleet.hasTag(Tags.NOT_RANDOM_MISSION_TARGET)
                || fleet.hasTag(Tags.SHIP_UNIQUE_SIGNATURE)
                || fleet.hasShipsWithUniqueSig()) {
            return true;
        }

        if (fleet.getFleetData() != null) {
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                if (isProtectedMember(member)) return true;
            }
        }

        // Salvage defenders and station fleets are normally referenced by the
        // entity they protect, rather than tagged on the fleet itself.
        return isReferencedByProtectedEntity(fleet);
    }

    private static boolean isProtectedMember(FleetMemberAPI member) {
        return member != null && ShipReplacer.isProtectedShip(member);
    }

    private static boolean hasAnyMemoryKey(MemoryAPI memory, String... keys) {
        if (memory == null) return false;
        for (String key : keys) {
            if (memory.contains(key)) return true;
        }
        return false;
    }

    private static boolean isReferencedByProtectedEntity(CampaignFleetAPI fleet) {
        LocationAPI location = fleet.getContainingLocation();
        if (location == null) return false;

        for (SectorEntityToken entity : location.getAllEntities()) {
            if (entity == null || entity == fleet) continue;
            MemoryAPI memory = entity.getMemoryWithoutUpdate();
            if (memory == null) continue;

            if ((memory.contains(DEFENDER_FLEET_KEY)
                    && memory.get(DEFENDER_FLEET_KEY) == fleet)
                    || (memory.contains(MemFlags.STATION_FLEET)
                    && memory.get(MemFlags.STATION_FLEET) == fleet)
                    || (memory.contains(MemFlags.STATION_BASE_FLEET)
                    && memory.get(MemFlags.STATION_BASE_FLEET) == fleet)) {
                return true;
            }
        }
        return false;
    }
}
