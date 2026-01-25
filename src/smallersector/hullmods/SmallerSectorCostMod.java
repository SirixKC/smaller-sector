package smallersector.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import smallersector.CostModifier;
import smallersector.Settings;

public class SmallerSectorCostMod extends BaseHullMod {

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
            MutableShipStatsAPI stats, String id) {
        CostModifier.applyToStats(stats, hullSize);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (hullSize == HullSize.CRUISER) {
            if (index == 0) return String.format("%.1fx", Settings.getCruiserCrewMult());
            if (index == 1) return String.format("%.1fx", Settings.getCruiserSupplyMult());
            if (index == 2) return String.format("%.1fx", Settings.getCruiserFuelMult());
        } else if (hullSize == HullSize.CAPITAL_SHIP) {
            if (index == 0) return String.format("%.1fx", Settings.getCapitalCrewMult());
            if (index == 1) return String.format("%.1fx", Settings.getCapitalSupplyMult());
            if (index == 2) return String.format("%.1fx", Settings.getCapitalFuelMult());
        }
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship == null) return false;
        HullSize size = ship.getHullSize();
        return size == HullSize.CRUISER || size == HullSize.CAPITAL_SHIP;
    }

    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return false; // Hidden from manual selection
    }
}
