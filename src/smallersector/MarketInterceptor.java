package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.ArrayList;
import java.util.List;

public class MarketInterceptor implements EconomyTickListener {

    @Override
    public void reportEconomyTick(int iterIndex) {
        // Process all markets on economy tick
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            processMarket(market);
        }
    }

    @Override
    public void reportEconomyMonthEnd() {
        // Not needed
    }

    public static void processMarket(MarketAPI market) {
        if (market == null || market.getFaction() == null) return;

        String factionId = market.getFaction().getId();

        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            processSubmarket(submarket, factionId);
        }
    }

    private static void processSubmarket(SubmarketAPI submarket, String factionId) {
        if (submarket == null || submarket.getCargo() == null) return;
        if (submarket.getCargo().getMothballedShips() == null) return;

        // Process ships for sale
        List<FleetMemberAPI> ships = submarket.getCargo().getMothballedShips().getMembersListCopy();
        List<FleetMemberAPI> toRemove = new ArrayList<FleetMemberAPI>();
        List<FleetMemberAPI> toAdd = new ArrayList<FleetMemberAPI>();

        for (FleetMemberAPI member : ships) {
            FleetMemberAPI replacement = ShipReplacer.tryReplace(member, factionId);
            if (replacement != null) {
                toRemove.add(member);
                toAdd.add(replacement);
            }
        }

        // Apply changes
        for (FleetMemberAPI member : toRemove) {
            submarket.getCargo().getMothballedShips().removeFleetMember(member);
        }
        for (FleetMemberAPI member : toAdd) {
            submarket.getCargo().getMothballedShips().addFleetMember(member);
        }
    }
}
