package at.jku.se.gruppe2.domain.model.device.actuator;

import at.jku.se.gruppe2.domain.model.device.config.CatFeederConfig;
import at.jku.se.gruppe2.domain.model.device.sensor.CatSensor;

/**
 * Actuator controlling an automated cat feeder.
 * The feeder operates based on confidence values provided by the
 * {@link CatSensor}. Feeding mechanism is triggered when the detected confidence
 * exceeds a configurable threshold.
 *
 * To avoid repeated feeding, a cooldown mechanism is applied that blocks
 * feeding for a defined number of simulation ticks
 */
public class CatFeederActuator extends Actuator {

    private final CatSensor catSensor;
    private CatFeederConfig config;

    private int cooldownRemainingTicks = 0;
    private int feedCount = 0; // optional für UI/Debug

    /**
     * Creates a fully configured cat feeder actuator.
     * @param id        actuator ID
     * @param name      human-readable name
     * @param catSensor sensor providing cat detection confidence
     * @param config    feeder configuration
     */
    public CatFeederActuator(int id, String name, CatSensor catSensor, CatFeederConfig config) {
        this.setId(id);
        this.setLabel(name);
        this.catSensor = catSensor;
        this.config = config;
        this.setState("IDLE");
    }

    /**
     * Creates a cat feeder actuator with only a sensor assigned.
     *
     * @param catSensor sensor providing cat detection confidence
     */

    public CatFeederActuator(CatSensor catSensor) {
        this.catSensor = catSensor;
    }

    /**
     * @return the feeder configuration
     */
    public CatFeederConfig getConfig() {
        return config;
    }

    /**
     * Sets the feeder configuration.
     *
     * @param config new configuration
     */
    public void setConfig(CatFeederConfig config) {
        this.config = config;
    }

    public int getCooldownRemainingTicks() {
        return cooldownRemainingTicks;
    }

    public int getFeedCount() {
        return feedCount;
    }

    /**
     * Advances the feeder logic by one simulation tick
     */
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
    }
}