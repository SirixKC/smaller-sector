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
        if (Global.getSector() == null) return;
        if (Global.getSector().getEconomy() == null) return;

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

        com.fs.starfarer.api.fleet.FleetDataAPI mothballedShips = submarket.getCargo().getMothballedShips();
        if (mothballedShips == null) return;

        // Process ships for sale
        List<FleetMemberAPI> ships = mothballedShips.getMembersListCopy();
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
            mothballedShips.removeFleetMember(member);
        }
        for (FleetMemberAPI member : toAdd) {
            mothballedShips.addFleetMember(member);
        }
    }
}
