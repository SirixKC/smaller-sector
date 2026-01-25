package smallersector;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionProductionAPI;
import com.fs.starfarer.api.campaign.FactionProductionAPI.ItemInProductionAPI;
import com.fs.starfarer.api.campaign.FactionProductionAPI.ProductionItemType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Monitors all faction production queues and applies time multipliers
 * to cruisers and capitals as they are queued.
 */
public class ProductionTimeModifier implements EveryFrameScript {

    private static final Logger log = Global.getLogger(ProductionTimeModifier.class);

    // Track which items we've already modified to avoid re-multiplying
    // Uses object identity since ItemInProductionAPI doesn't have a stable ID
    private Set<ItemInProductionAPI> modifiedItems = new HashSet<>();

    // Store original production capacity for global multiplier
    private static float originalProductionCapacity = -1f;

    @Override
    public boolean isDone() {
        return false; // Run forever
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        // Apply global production multiplier (affects capacity, thus time)
        applyGlobalMultiplier();

        // Scan all factions for new production items
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            FactionProductionAPI production = faction.getProduction();
            if (production == null) continue;

            for (ItemInProductionAPI item : production.getCurrent()) {
                processProductionItem(item);
            }
        }

        // Clean up references to completed items to prevent memory leak
        cleanupModifiedSet();
    }

    /**
     * Apply the global production multiplier by modifying productionCapacityPerSWUnit.
     * Lower capacity = longer build times for all ships.
     */
    private void applyGlobalMultiplier() {
        float globalMult = Settings.getGlobalProductionMult();

        // Store original value on first run
        if (originalProductionCapacity < 0) {
            originalProductionCapacity = Global.getSettings().getFloat("productionCapacityPerSWUnit");
            log.info("Smaller Sector: Stored original productionCapacityPerSWUnit: " + originalProductionCapacity);
        }

        // Apply multiplier (divide capacity to increase time)
        float newCapacity = originalProductionCapacity / globalMult;
        Global.getSettings().setFloat("productionCapacityPerSWUnit", newCapacity);
    }

    /**
     * Process a single production item, applying per-size multipliers if needed.
     */
    private void processProductionItem(ItemInProductionAPI item) {
        // Skip if already processed
        if (modifiedItems.contains(item)) {
            return;
        }

        // Only process ships
        if (item.getType() != ProductionItemType.SHIP) {
            return;
        }

        ShipHullSpecAPI spec = item.getShipSpec();
        if (spec == null) {
            return;
        }

        HullSize size = spec.getHullSize();
        if (size != HullSize.CRUISER && size != HullSize.CAPITAL_SHIP) {
            // Mark as processed so we don't check again
            modifiedItems.add(item);
            return;
        }

        // Get the per-size multiplier (not including global, which is handled separately)
        float sizeMult;
        if (size == HullSize.CRUISER) {
            sizeMult = Settings.getCruiserProductionMult();
        } else {
            sizeMult = Settings.getCapitalProductionMult();
        }

        // Only modify if multiplier is > 1
        if (sizeMult > 1.001f) {
            float baseBuildDelay = item.getBaseBuildDelay();
            float currentBuildDelay = item.getBuildDelay();

            // Debug: log all values
            log.info("Smaller Sector: " + spec.getHullName() + " - baseBuildDelay=" + baseBuildDelay +
                    ", currentBuildDelay=" + currentBuildDelay + ", baseCost=" + item.getBaseCost());

            // If baseBuildDelay is 0, try using current delay
            float effectiveBase = baseBuildDelay;
            if (effectiveBase < 0.001f) {
                effectiveBase = currentBuildDelay;
            }

            // If still 0, we can't modify it meaningfully
            if (effectiveBase < 0.001f) {
                log.info("Smaller Sector: Cannot modify - no valid build delay found");
                modifiedItems.add(item);
                return;
            }

            float newBuildDelay = effectiveBase * sizeMult;
            item.setBuildDelay(newBuildDelay);

            log.info("Smaller Sector: Modified production time for " + spec.getHullName() +
                    " (" + size + "): " + effectiveBase + " -> " + newBuildDelay +
                    " (x" + sizeMult + ")");
        }

        // Mark as processed
        modifiedItems.add(item);
    }

    /**
     * Remove references to items that are no longer in any production queue.
     * This prevents memory leaks from holding references to completed items.
     */
    private void cleanupModifiedSet() {
        // Only clean up occasionally to avoid performance impact
        if (modifiedItems.size() < 100) {
            return;
        }

        Set<ItemInProductionAPI> currentItems = new HashSet<>();
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            FactionProductionAPI production = faction.getProduction();
            if (production == null) continue;
            currentItems.addAll(production.getCurrent());
        }

        modifiedItems.retainAll(currentItems);
    }

    /**
     * Called when settings change - reapply global multiplier.
     */
    public static void onSettingsChanged() {
        if (originalProductionCapacity > 0) {
            float globalMult = Settings.getGlobalProductionMult();
            float newCapacity = originalProductionCapacity / globalMult;
            Global.getSettings().setFloat("productionCapacityPerSWUnit", newCapacity);
            log.info("Smaller Sector: Updated productionCapacityPerSWUnit to " + newCapacity +
                    " (global mult: " + globalMult + ")");
        }
    }
}
