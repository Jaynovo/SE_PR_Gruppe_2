package at.jku.se.gruppe2.domain.model.device.sensor;

/**
 * CatSensor:
 * - value (inherited from Sensor) represents the confidence (0.0 .. 1.0) that a cat is present.
 * - A cat is considered detected if confidence >= threshold.
 */
public class CatSensor extends Sensor {

    // default threshold; can be changed via setter/config file later
    private double detectionThreshold = 0.75;
    private String lastImageUrl;

    public double getDetectionThreshold() {
        return detectionThreshold;
    }

    public void setDetectionThreshold(double detectionThreshold) {
        if (detectionThreshold < 0.0) detectionThreshold = 0.0;
        if (detectionThreshold > 1.0) detectionThreshold = 1.0;
        this.detectionThreshold = detectionThreshold;
    }

    /**
     * Convenience method: set confidence (0..1)
     */
    public void setConfidence(double confidence) {
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
        setValue(confidence);
    }

    /**
     * Returns current confidence (0..1)
     */
    public double getConfidence() {
        return getValue();
    }

    /**
     * True if confidence >= threshold
     */
    public boolean isCatDetected() {
        return getConfidence() >= detectionThreshold;
    }

    public String getLastImageUrl() {
        return lastImageUrl;
    }

    public void updateDetection(double confidence, String imageUrl) {
        setConfidence(confidence);
        this.lastImageUrl = imageUrl;
    }
}
