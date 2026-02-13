package at.jku.se.gruppe2.domain.model.device.actuator;

import java.util.Locale;

/**
 * Actuator representing window blinds with adjustable position in %.
 * The blinds position is encoded in the actuator state as {@code "POS=<0..100>"} where:
 * 0 = fully closed
 * 100 = fully open
 * Invalid or missing state values are handled gracefully and default to a fully open position.
 */
public class BlindsActuator extends Actuator {

    private static final int DEFAULT_POSITION = 100;

    public BlindsActuator() {
        setPosition(DEFAULT_POSITION);
    }
    /**
     * Returns the current blinds position.
     *
     * @return position between 0 and 100
     */
    public int getPosition() {
        Integer pos = parseIntKV(safe(getState()), "POS");
        if (pos == null) return DEFAULT_POSITION;
        return clamp(pos, 0, DEFAULT_POSITION);
    }
    /**
     * Sets the blinds position.
     *
     * @param pos desired position (values outside 0..100 are clamped)
     */
    public void setPosition(int pos) {
        int p = clamp(pos, 0, DEFAULT_POSITION);
        setState(String.format(Locale.ROOT, "POS=%d", p));
    }

    public void openBlinds() {setPosition(100);}
    public void closeBlinds() {setPosition(0);}

    public boolean isOpen() { return getPosition()>=100;}
    public boolean isClosed() { return getPosition()<=0;}

    // --- helper methods ---
    private static Integer parseIntKV(String state, String key) {
        if (state.isBlank()) return null;
        String needle = key.toUpperCase(Locale.ROOT) + "=";

        for (String part : state.split(";")) {
            String p = part.trim();
            if (p.toUpperCase(Locale.ROOT).startsWith(needle)) {
                String val = p.substring(needle.length()).trim();
                try {
                    return Integer.parseInt(val);
                } catch (NumberFormatException ignored) {
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
        return (s == null) ? "" : s.trim();
    }
}
