package at.jku.se.gruppe2.model.actuator;

import java.util.Locale;

public class BlindsActuator extends Actuator {

    private static final int DEFAULT_POSITION = 100;

    public BlindsActuator() {
        setPosition(DEFAULT_POSITION);
    }

    public int getPosition() {
        Integer pos = parseIntKV(safe(getState()), "POS");
        if (pos == null) return DEFAULT_POSITION;
        return clamp(pos, 0, DEFAULT_POSITION);
    }

    public void setPosition(int pos) {
        int p = clamp(pos, 0, DEFAULT_POSITION);
        setState(String.format(Locale.ROOT, "POS=%d", p));
    }

    public void openBlinds() {setPosition(100);}
    public void closeBlinds() {setPosition(0);}

    public boolean isOpen() { return getPosition()>=100;}
    public boolean isClosed() { return getPosition()<=0;}

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
