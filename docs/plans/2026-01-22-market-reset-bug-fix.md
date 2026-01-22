# Market Reset Bug Fix Design

## Problem Statement

The mod has two bugs related to market ship replacement:

1. **Markets not processed on first encounter** - `MarketInterceptor` uses `EconomyTickListener` which only fires monthly. When discovering a new market mid-game, ships aren't processed until the next economy tick.

2. **Ships re-roll on every reload** - Unlike `FleetInterceptor` and `DerelictInterceptor` which tag processed entities, `MarketInterceptor` has no tracking. Every reload triggers fresh random rolls on all market ships.

## Desired Behavior

- Process ships **once per natural stock refresh cycle**
- First encounter = process immediately
- Save/reload = ships stay the same (stable results)
- Game refreshes stock naturally = process the new stock once

## Solution

### Core Detection Formula

Track the absolute game day when we last processed each submarket. To detect if a refresh happened:

```
shouldProcess = (currentDay - lastProcessedDay) > sinceLastCargoUpdate
```

**Why this works:**
- `sinceLastCargoUpdate` = days since the most recent refresh
- Last refresh happened on `currentDay - sinceLastCargoUpdate`
- If `lastProcessedDay` was before that refresh, we need to process

**Examples:**

| Scenario | currentDay | lastProcessed | sinceLastCargoUpdate | Check | Result |
|----------|------------|---------------|---------------------|-------|--------|
| Just visited | 100 | 98 | 15 | `2 > 15?` No | Skip |
| One refresh passed | 150 | 100 | 20 | `50 > 20?` Yes | Process |
| Three refreshes passed | 200 | 100 | 5 | `100 > 5?` Yes | Process |

### Data Storage

```java
private static final String PERSISTENCE_KEY = "smallersector_market_processed";

// Stored in Global.getSector().getPersistentData()
// Map structure: submarketKey → lastProcessedDay (Float)
// Example: "jangala_open_market" → 145.0f
```

**Save compatibility:** Only primitive types (String, Float) are stored, so removing the mod won't break saves.

### Trigger Points

| Trigger | Purpose | Implementation |
|---------|---------|----------------|
| Player opens market | First encounter detection | `ColonyInteractionListener.reportPlayerOpenedMarket()` |
| Economy tick | Catch refreshes while playing | Existing `EconomyTickListener.reportEconomyTick()` |
| Game load | Process any missed refreshes | Existing `onGameLoad()` call |

### Submarket Identification

Each submarket tracked independently using composite key:

```java
String submarketKey = market.getId() + "_" + submarket.getSpecId();
// Examples: "jangala_open_market", "sindria_black_market", "chicomoztoc_military_market"
```

## Implementation

### Files to Modify

1. **`MarketInterceptor.java`** - Add `ColonyInteractionListener`, persistence logic, timestamp checking

### MarketInterceptor Changes

```java
public class MarketInterceptor implements EconomyTickListener, ColonyInteractionListener {

    private static final String PERSISTENCE_KEY = "smallersector_market_processed";

    // ========== Persistence ==========

    @SuppressWarnings("unchecked")
    private static Map<String, Float> getProcessedMap() {
        Map<String, Object> persistent = Global.getSector().getPersistentData();
        Map<String, Float> map = (Map<String, Float>) persistent.get(PERSISTENCE_KEY);
        if (map == null) {
            map = new HashMap<String, Float>();
            persistent.put(PERSISTENCE_KEY, map);
        }
        return map;
    }

    private static String getSubmarketKey(MarketAPI market, SubmarketAPI submarket) {
        return market.getId() + "_" + submarket.getSpecId();
    }

    private static boolean shouldProcess(MarketAPI market, SubmarketAPI submarket) {
        String key = getSubmarketKey(market, submarket);
        Map<String, Float> processed = getProcessedMap();

        float currentDay = Global.getSector().getClock().getTimestamp() / (24f * 60f * 60f);
        float sinceLastUpdate = submarket.getSinceLastCargoUpdate();

        Float lastProcessed = processed.get(key);
        if (lastProcessed == null) return true;  // Never processed

        return (currentDay - lastProcessed) > sinceLastUpdate;
    }

    private static void markProcessed(MarketAPI market, SubmarketAPI submarket) {
        String key = getSubmarketKey(market, submarket);
        float currentDay = Global.getSector().getClock().getTimestamp() / (24f * 60f * 60f);
        getProcessedMap().put(key, currentDay);
    }

    // ========== Listeners ==========

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        processMarket(market);
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        // Not needed
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        if (Global.getSector() == null) return;
        if (Global.getSector().getEconomy() == null) return;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            processMarket(market);
        }
    }

    @Override
    public void reportEconomyMonthEnd() {
        // Not needed
    }

    // ========== Processing ==========

    public static void processMarket(MarketAPI market) {
        if (market == null || market.getFaction() == null) return;

        String factionId = market.getFaction().getId();

        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (!shouldProcess(market, submarket)) continue;

            processSubmarket(submarket, factionId);
            markProcessed(market, submarket);
        }
    }

    private static void processSubmarket(SubmarketAPI submarket, String factionId) {
        // Existing ship replacement logic unchanged
    }
}
```

## Testing Checklist

- [ ] New game: First market encounter processes ships immediately
- [ ] Save/reload: Ships remain the same (no re-rolling)
- [ ] Wait 30+ days: Ships re-process after natural stock refresh
- [ ] Multiple submarkets: Each tracked independently (open, black, military)
- [ ] Mod removal: Save loads without errors
