package at.jku.se.gruppe2.domain.model.device.sensor;

/**
 * CatSensor:
 * - value (inherited from Sensor) represents the confidence (0.0 .. 1.0) that a cat is present.
 * - A cat is considered detected if confidence >= threshold.
 */
public class CatSensor extends Sensor {

    /**
     * Default detection threshold (clamped to 0..1).
     */
    private double detectionThreshold = 0.75;

    /**
     * Optional URL of the last image used for detection (e.g., from a camera service).
     */
    private String lastImageUrl;
    /**
     * @return detection threshold (0..1)
     */
    public double getDetectionThreshold() {
        return detectionThreshold;
    }
    /**
     * Sets the detection threshold.
     * Values are clamped to the range 0..1.
     *
     * @param detectionThreshold threshold in range 0..1
     */
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
     * @return  current confidence (0..1)
     */
    public double getConfidence() {
        return getValue();
    }

    /**
     * Checks whether the confidence indicates a detected cat
     * @return {@code true} if {@code confidence >= detectionThreshold}, otherwise {@code false}
     */
    public boolean isCatDetected() {
        return getConfidence() >= detectionThreshold;
    }
    /**
     * @return URL of the last image used for detection (may be {@code null})
     */
    public String getLastImageUrl() {
        return lastImageUrl;
    }
    /**
     * Updates the detection result (confidence + optional image URL).
     *
     * @param confidence confidence in range 0..1
     * @param imageUrl   URL of the image used for detection (may be {@code null})
     */
    public void updateDetection(double confidence, String imageUrl) {
        setConfidence(confidence);
        this.lastImageUrl = imageUrl;
    }
}
