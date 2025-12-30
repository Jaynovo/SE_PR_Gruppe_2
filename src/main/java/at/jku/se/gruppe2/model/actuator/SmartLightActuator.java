package at.jku.se.gruppe2.model.actuator;

import java.util.Locale;
import java.util.Objects;

/**
 * Actuator representing a dimmable smart light.
 *
 * <p>Persistence constraint: the database stores actuator state as a single String.
 * Therefore this actuator encodes both power and brightness into {@code state}:
 *
 * <ul>
 *   <li>{@code "OFF"} means light is off (brightness effectively 0)</li>
 *   <li>{@code "ON;B=<0..100>"} means light is on with brightness 0..100</li>
 * </ul>
 *
 * <p>Note: {@code getBrightness()} returns 0 for OFF/empty state and a default if parsing fails.</p>
 */
public class SmartLightActuator extends Actuator {
    public static final String ON = "ON";
    public static final String OFF = "OFF";
    private static final int DEFAULT_BRIGHTNESS = 100;

    public SmartLightActuator() {
        setState(OFF);
    }

    public boolean isOn() {
        return (Objects.equals(getState(), ON));
    }

    public void turnOn() {
        int b = getBrightness();
        if (b <= 0) b = DEFAULT_BRIGHTNESS;
        setState(encodeOn(b));
    }

    public void turnOff() {
        setState(OFF);
    }

    public void toggle() {
        if (isOn()) turnOff();
        else turnOn();
    }

    public int getBrightness() {
        String s = safe(getState());
        if (s.equalsIgnoreCase(OFF) || s.isEmpty()) return 0;
        Integer b = parseInt(s, "B");
        if (b == null) return DEFAULT_BRIGHTNESS;
        return clamp(b, 0, 100);
    }

    public void setBrightness(int brightness) {
        int b = clamp(brightness, 0, 100);
        if (b < 0) b = 0;
        if (b > 100) b = 100;
        if (b == 0) turnOff();
        else setState(encodeOn(b));
    }

    private static String encodeOn(int brightness) {
        return String.format(Locale.ROOT, "%s;B=%d", ON, clamp(brightness, 0, 100));
    }

    /**
     * Extracts an integer value from a semicolon-separated state string.
     *
     * <p>Example state: {@code "ON;B=75"} with key {@code "B"} will return 75.</p>
     *
     * <p>Implementation detail: state is split by ';' and each part is trimmed.
     * Matching is intended to be case-insensitive via lowercasing the "needle".</p>
     *
     * @param state the full state String (may be null)
     * @param key   the key name (e.g., "B")
     * @return parsed Integer or null if key not present or value is not a valid integer
     */
    private static Integer parseInt(String state, String key) {
        String s = safe(state);
        String needle = key.toUpperCase(Locale.ROOT) + "=";

        for (String part : s.split(";")) {
            String p = part.trim();
            if (p.toUpperCase(Locale.ROOT).startsWith(needle)) {
                String value = p.substring(needle.length()).trim();
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
