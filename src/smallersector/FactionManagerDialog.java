package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import lunalib.lunaSettings.LunaSettings;
import org.lazywizard.lazylib.JSONUtils;

import java.awt.Color;
import java.util.*;

/**
 * Dialog for managing the faction blacklist.
 * Shows all loaded factions with toggle options.
 */
public class FactionManagerDialog implements InteractionDialogPlugin {

    private static final String MOD_ID = "smallersector";

    private InteractionDialogAPI dialog;
    private Set<String> currentBlacklist;
    private List<FactionAPI> allFactions;
    private int currentPage = 0;
    private static final int FACTIONS_PER_PAGE = 8;

    // Option IDs
    private static final String OPT_PREV_PAGE = "prev_page";
    private static final String OPT_NEXT_PAGE = "next_page";
    private static final String OPT_SAVE = "save";
    private static final String OPT_CANCEL = "cancel";
    private static final String OPT_TOGGLE_PREFIX = "toggle_";

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;

        // Load current blacklist
        currentBlacklist = new HashSet<>(Settings.getFactionBlacklist());

        // Get all factions, sorted by name
        allFactions = new ArrayList<>();
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            // Skip player faction and factions with no ships
            if (faction.isPlayerFaction()) continue;
            if ("neutral".equals(faction.getId())) continue;
            if ("player".equals(faction.getId())) continue;

            allFactions.add(faction);
        }

        // Sort by display name
        allFactions.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));

        showFactionList();
    }

    private void showFactionList() {
        dialog.getTextPanel().clear();
        dialog.getOptionPanel().clearOptions();

        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color positive = Misc.getPositiveHighlightColor();
        Color negative = Misc.getNegativeHighlightColor();

        // Header
        dialog.getTextPanel().addPara("Faction Blacklist Manager", h);
        dialog.getTextPanel().addPara("Blacklisted factions will NOT have their ships replaced. " +
                "Click a faction to toggle its blacklist status.", g);
        dialog.getTextPanel().addPara("", g);

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) allFactions.size() / FACTIONS_PER_PAGE);
        int startIndex = currentPage * FACTIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + FACTIONS_PER_PAGE, allFactions.size());

        // Page indicator
        dialog.getTextPanel().addPara("Page " + (currentPage + 1) + " of " + totalPages +
                " (" + allFactions.size() + " factions total)", g);
        dialog.getTextPanel().addPara("", g);

        // Legend
        dialog.getTextPanel().addPara("[BLACKLISTED] = Protected from replacement", positive);
        dialog.getTextPanel().addPara("[ACTIVE] = Ships will be replaced", negative);
        dialog.getTextPanel().addPara("", g);

        // Show factions for current page
        for (int i = startIndex; i < endIndex; i++) {
            FactionAPI faction = allFactions.get(i);
            boolean isBlacklisted = currentBlacklist.contains(faction.getId().toLowerCase());

            String status = isBlacklisted ? "[BLACKLISTED]" : "[ACTIVE]";
            Color statusColor = isBlacklisted ? positive : negative;

            String optionText = faction.getDisplayName() + " - " + status;
            dialog.getOptionPanel().addOption(optionText, OPT_TOGGLE_PREFIX + faction.getId());

            // Note: OptionPanelAPI doesn't support per-option colors, status shown in text
        }

        dialog.getOptionPanel().addOption("", "spacer1");
        dialog.getOptionPanel().setEnabled("spacer1", false);

        // Navigation
        if (currentPage > 0) {
            dialog.getOptionPanel().addOption("Previous Page", OPT_PREV_PAGE);
        }
        if (currentPage < totalPages - 1) {
            dialog.getOptionPanel().addOption("Next Page", OPT_NEXT_PAGE);
        }

        dialog.getOptionPanel().addOption("", "spacer2");
        dialog.getOptionPanel().setEnabled("spacer2", false);

        // Save/Cancel
        int blacklistCount = currentBlacklist.size();
        dialog.getOptionPanel().addOption("Save Changes (" + blacklistCount + " blacklisted)", OPT_SAVE);
        dialog.getOptionPanel().addOption("Cancel", OPT_CANCEL);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionData == null) return;

        String option = optionData.toString();

        if (option.startsWith(OPT_TOGGLE_PREFIX)) {
            // Toggle faction blacklist status
            String factionId = option.substring(OPT_TOGGLE_PREFIX.length()).toLowerCase();
            if (currentBlacklist.contains(factionId)) {
                currentBlacklist.remove(factionId);
            } else {
                currentBlacklist.add(factionId);
            }
            showFactionList();

        } else if (OPT_PREV_PAGE.equals(option)) {
            currentPage--;
            showFactionList();

        } else if (OPT_NEXT_PAGE.equals(option)) {
            currentPage++;
            showFactionList();

        } else if (OPT_SAVE.equals(option)) {
            saveBlacklist();
            dialog.dismiss();

        } else if (OPT_CANCEL.equals(option)) {
            dialog.dismiss();
        }
    }

    private void saveBlacklist() {
        try {
            // Build comma-separated string
            StringBuilder sb = new StringBuilder();
            for (String factionId : currentBlacklist) {
                if (sb.length() > 0) sb.append(",");
                sb.append(factionId);
            }
            String blacklistString = sb.toString();

            // Save to LunaSettings JSON
            JSONUtils.CommonDataJSONObject data = JSONUtils.loadCommonJSON(
                    "LunaSettings/" + MOD_ID + ".json",
                    "data/config/LunaSettingsDefault.default"
            );

            if (data != null) {
                data.put("factionBlacklist", blacklistString);

                // Also set preset to Custom since user is manually editing
                data.put("preset", "Custom");
                data.save();
            }

            // Reload settings
            Settings.reloadBlacklist();

            Global.getSector().getCampaignUI().addMessage(
                    "Faction blacklist saved. " + currentBlacklist.size() + " factions protected.",
                    Misc.getPositiveHighlightColor());

        } catch (Exception e) {
            Global.getLogger(this.getClass()).error("Failed to save blacklist", e);
            Global.getSector().getCampaignUI().addMessage(
                    "Error saving blacklist: " + e.getMessage(),
                    Misc.getNegativeHighlightColor());
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
