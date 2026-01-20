package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import java.util.ArrayList;
import java.util.List;

public class BlueprintInterceptor {

    /**
     * Process cargo for blueprint replacement.
     * Call this when loot is generated.
     */
    public static void processCargo(CargoAPI cargo, String factionId) {
        if (cargo == null || factionId == null) return;

        List<CargoStackAPI> toRemove = new ArrayList<CargoStackAPI>();
        List<SpecialItemData> toAdd = new ArrayList<SpecialItemData>();

        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (!stack.isSpecialStack()) continue;

            SpecialItemData special = stack.getSpecialDataIfSpecial();
            if (special == null) continue;

            // Check if it's a ship blueprint
            if ("ship_bp".equals(special.getId())) {
                String hullId = special.getData();
                String replacement = ShipReplacer.tryReplaceBlueprintHull(hullId, factionId);

                if (replacement != null) {
                    toRemove.add(stack);
                    toAdd.add(new SpecialItemData("ship_bp", replacement));
                }
            }
        }

        // Apply changes
        for (CargoStackAPI stack : toRemove) {
            cargo.removeStack(stack);
        }
        for (SpecialItemData item : toAdd) {
            cargo.addSpecial(item, 1);
        }
    }

    /**
     * Process a blueprint package - each blueprint checked individually.
     */
    public static void processBlueprintPackage(CargoAPI cargo, String factionId, List<String> hullIds) {
        if (cargo == null || factionId == null || hullIds == null) return;

        for (int i = 0; i < hullIds.size(); i++) {
            String hullId = hullIds.get(i);
            String replacement = ShipReplacer.tryReplaceBlueprintHull(hullId, factionId);
            if (replacement != null) {
                hullIds.set(i, replacement);
            }
        }
    }
}
