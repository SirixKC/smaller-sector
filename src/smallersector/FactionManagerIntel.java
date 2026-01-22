package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Set;

/**
 * Intel item that provides access to the Faction Blacklist Manager.
 * Appears in the Intel screen when a game is loaded.
 */
public class FactionManagerIntel extends BaseIntelPlugin {

    private static final String BUTTON_MANAGE = "manage_blacklist";

    public FactionManagerIntel() {
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        float pad = 3f;

        info.addPara(getName(), getTitleColor(mode), 0f);

        if (mode == ListInfoMode.IN_DESC) {
            int blacklistCount = Settings.getFactionBlacklist().size();
            info.addPara("Managing ship replacement for factions.", pad, g);
            info.addPara("%s factions currently blacklisted.", pad, h, String.valueOf(blacklistCount));
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        float pad = 10f;

        info.addPara("The Smaller Sector mod replaces cruisers and capitals with smaller ships " +
                "to create a more frigate/destroyer-focused experience.", pad);

        info.addPara("Use the Faction Blacklist Manager to choose which factions are " +
                "exempt from ship replacement.", pad);

        // Show current blacklist status
        Set<String> blacklist = Settings.getFactionBlacklist();
        if (blacklist.isEmpty()) {
            info.addPara("Currently: No factions blacklisted - all factions will have ships replaced.",
                    pad, Misc.getNegativeHighlightColor(), "No factions blacklisted");
        } else {
            info.addPara("Currently: %s factions blacklisted.", pad, h, String.valueOf(blacklist.size()));

            // List a few examples
            StringBuilder examples = new StringBuilder();
            int count = 0;
            for (String factionId : blacklist) {
                if (count >= 5) {
                    examples.append("...");
                    break;
                }
                FactionAPI faction = Global.getSector().getFaction(factionId);
                String name = faction != null ? faction.getDisplayName() : factionId;
                if (count > 0) examples.append(", ");
                examples.append(name);
                count++;
            }
            info.addPara("Including: " + examples.toString(), pad, g);
        }

        // Add the manage button
        info.addSpacer(pad);
        ButtonAPI button = info.addButton("Open Faction Manager", BUTTON_MANAGE,
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                width - 20, 25f, pad);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (BUTTON_MANAGE.equals(buttonId)) {
            // Open our faction manager dialog
            ui.showDialog(Global.getSector().getPlayerFleet(), new FactionManagerDialog());
        }
    }

    @Override
    public boolean hasSmallDescription() {
        return true;
    }

    @Override
    public boolean hasLargeDescription() {
        return false;
    }

    @Override
    public String getName() {
        return "Smaller Sector - Faction Manager";
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "fleet_log");
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("Missions"); // Put it in the Missions tab
        return tags;
    }

    @Override
    public IntelSortTier getSortTier() {
        return IntelSortTier.TIER_0; // Show at top
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return null; // No map location
    }

    @Override
    public boolean shouldRemoveIntel() {
        return false; // Permanent intel item
    }

    /**
     * Check if this intel already exists in the sector
     */
    public static boolean exists() {
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel()) {
            if (intel instanceof FactionManagerIntel) {
                return true;
            }
        }
        return false;
    }
}
