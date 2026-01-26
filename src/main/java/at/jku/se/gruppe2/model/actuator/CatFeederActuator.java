package at.jku.se.gruppe2.model.actuator;

import at.jku.se.gruppe2.model.actuator.Actuator;
import at.jku.se.gruppe2.model.sensor.CatSensor;

public class CatFeederActuator extends Actuator {

    private final CatSensor catSensor;
    private CatFeederConfig config;

    private int cooldownRemainingTicks = 0;
    private int feedCount = 0; // optional für UI/Debug

    public CatFeederActuator(int id, String name, CatSensor catSensor, CatFeederConfig config) {
        this.setId(id);
        this.setLabel(name);
        this.catSensor = catSensor;
        this.config = config;
        this.setState("IDLE");
    }

    public CatFeederActuator(CatSensor catSensor) {
        this.catSensor = catSensor;
    }

    public CatFeederConfig getConfig() {
        return config;
    }

    public void setConfig(CatFeederConfig config) {
        this.config = config;
    }

    public int getCooldownRemainingTicks() {
        return cooldownRemainingTicks;
    }

    public int getFeedCount() {
        return feedCount;
    }

    /** Wird pro Tick (alle 2 Sekunden) aufgerufen */
    public void tick() {
        if (cooldownRemainingTicks > 0) {
            cooldownRemainingTicks--;
            this.setState("COOLDOWN(" + cooldownRemainingTicks + ")");
            return;
        }

        // Sensorwert holen
        double confidence = catSensor.getConfidence(); //0..1

        // Prüfen: ist die Katze "sicher genug" erkannt?
        if (confidence >= config.getMinConfidence()) {
            feed();
            cooldownRemainingTicks = Math.max(0, config.getCooldownTicks());
            this.setState("FED");
        } else {
            this.setState("IDLE");
        }
    }

    private void feed() {
        feedCount++;
        /*
        System.out.println("[CatFeeder] Feeding! count=" + feedCount
                + " (confidence=" + catSensor.getConfidence() + ")");
                */
    }
}