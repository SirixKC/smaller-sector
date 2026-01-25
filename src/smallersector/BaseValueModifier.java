package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.apache.log4j.Logger;
import org.magiclib.ReflectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modifies ship base values (production/purchase cost) for cruisers and capitals.
 * Uses MagicLib's ReflectionUtils to bypass security restrictions.
 */
public class BaseValueModifier {

    private static final Logger log = Global.getLogger(BaseValueModifier.class);

    // Maps hull ID -> original base value (before any modification)
    private static Map<String, Float> originalBaseValues = new HashMap<>();

    // Cache the obfuscated field name once found
    private static String baseValueFieldName = null;
    private static boolean searchFailed = false;

    /**
     * Store original base values for all cruisers and capitals.
     * Call this once during onApplicationLoad, before applying any multipliers.
     */
    public static void storeOriginalValues() {
        originalBaseValues.clear();

        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            HullSize size = spec.getHullSize();
            if (size == HullSize.CRUISER || size == HullSize.CAPITAL_SHIP) {
                originalBaseValues.put(spec.getHullId(), spec.getBaseValue());
            }
        }

        log.info("Smaller Sector: Stored original base values for " + originalBaseValues.size() + " cruisers/capitals.");
    }

    /**
     * Apply build cost multipliers to all cruisers and capitals.
     * Uses stored original values, so safe to call multiple times.
     */
    public static void applyMultipliers() {
        if (searchFailed) {
            return;
        }

        if (originalBaseValues.isEmpty()) {
            log.warn("Smaller Sector: No original values stored. Call storeOriginalValues() first.");
            return;
        }

        int modified = 0;
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            String hullId = spec.getHullId();
            Float originalValue = originalBaseValues.get(hullId);

            if (originalValue == null) {
                continue;
            }

            HullSize size = spec.getHullSize();
            float multiplier = Settings.getBuildCostMult(size);
            float newValue = originalValue * multiplier;

            // Log first few ships for debugging
            if (modified < 3) {
                log.info("Smaller Sector: Build cost [" + hullId + "] (" + size + "): " +
                         originalValue + " * " + multiplier + " = " + newValue);
            }

            if (setBaseValue(spec, newValue)) {
                modified++;
            }
        }

        log.info("Smaller Sector: Applied build cost multipliers to " + modified + " ships.");
    }

    /**
     * Restore original base values.
     */
    public static void restoreOriginalValues() {
        if (searchFailed) {
            return;
        }

        int restored = 0;
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            String hullId = spec.getHullId();
            Float originalValue = originalBaseValues.get(hullId);

            if (originalValue != null) {
                if (setBaseValue(spec, originalValue)) {
                    restored++;
                }
            }
        }

        log.info("Smaller Sector: Restored original base values for " + restored + " ships.");
    }

    /**
     * Set the base value on a hull spec using MagicLib's ReflectionUtils.
     * Uses getFieldsOfType to get field names (returns List<String>) then
     * get/set by name to avoid directly referencing java.lang.reflect.Field.
     */
    private static boolean setBaseValue(ShipHullSpecAPI spec, float newValue) {
        try {
            // If we already know the field name, use it directly
            if (baseValueFieldName != null) {
                ReflectionUtils.INSTANCE.set(baseValueFieldName, spec, newValue);
                return true;
            }

            // Find the field by searching for float fields matching getBaseValue()
            float currentValue = spec.getBaseValue();

            // Use getFieldsOfType which returns List<String> of field names
            // This avoids referencing java.lang.reflect.Field directly
            List<String> floatFieldNames = ReflectionUtils.INSTANCE.getFieldsOfType(spec, float.class);

            // Debug: log what we're looking for (only first ship)
            if (originalBaseValues.size() > 0 && baseValueFieldName == null) {
                log.info("Smaller Sector: Looking for base value " + currentValue +
                         " in " + spec.getHullId() + ", found " + floatFieldNames.size() + " float fields");

                // Log all field names and values for debugging
                StringBuilder sb = new StringBuilder("Field values: ");
                for (String fieldName : floatFieldNames) {
                    try {
                        Object val = ReflectionUtils.INSTANCE.get(fieldName, spec);
                        sb.append(fieldName).append("=").append(val).append(", ");
                    } catch (Exception ignored) {}
                }
                log.info("Smaller Sector: " + sb.toString());
            }

            for (String fieldName : floatFieldNames) {
                try {
                    Object value = ReflectionUtils.INSTANCE.get(fieldName, spec);
                    if (value instanceof Float) {
                        float floatVal = (Float) value;
                        if (Math.abs(floatVal - currentValue) < 0.01f) {
                            // Found it - cache the field name
                            baseValueFieldName = fieldName;
                            ReflectionUtils.INSTANCE.set(fieldName, spec, newValue);
                            log.info("Smaller Sector: Found base value field: " + fieldName);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    // Skip this field
                }
            }

            // Also try Float.TYPE (primitive float)
            List<String> floatFieldNames2 = ReflectionUtils.INSTANCE.getFieldsOfType(spec, Float.TYPE);

            for (String fieldName : floatFieldNames2) {
                try {
                    Object value = ReflectionUtils.INSTANCE.get(fieldName, spec);
                    if (value instanceof Float) {
                        float floatVal = (Float) value;
                        if (Math.abs(floatVal - currentValue) < 0.01f) {
                            baseValueFieldName = fieldName;
                            ReflectionUtils.INSTANCE.set(fieldName, spec, newValue);
                            log.info("Smaller Sector: Found base value field: " + fieldName);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    // Skip this field
                }
            }

            log.warn("Smaller Sector: Could not find base value field via MagicLib reflection.");
            searchFailed = true;
            return false;

        } catch (Exception e) {
            log.error("Smaller Sector: MagicLib reflection failed: " + e.getMessage(), e);
            searchFailed = true;
            return false;
        }
    }

    /**
     * Check if MagicLib reflection is working.
     */
    public static boolean isReflectionWorking() {
        return !searchFailed;
    }
}
