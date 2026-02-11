package at.jku.se.gruppe2.domain.model.device.sensor;

import java.time.Instant;

/**
 * Detects movement in a room
 * Returns boolean values: 1 = motion detected, 0 = no motion
 */
public class MotionSensor extends Sensor {

    private boolean motionDetected;
    private Instant lastMotionTime;
    private int detectionCount;

    /**
     * Creates a motion sensor with no detection and counter set to 0.
     */
    public MotionSensor() {
        super();
        this.motionDetected = false;
        this.detectionCount = 0;
    }

    /**
     * @return {@code true} if motion is currently detected
     */
    public boolean isMotionDetected() {
        return motionDetected;
    }

    /**
     * Updates the motion detected flag.
     * If motion is set to {@code true}, the current timestamp is stored and the detection counter is incremented.
     *
     * @param motionDetected new motion state
     */
    public void setMotionDetected(boolean motionDetected) {
        this.motionDetected = motionDetected;
        if (motionDetected) {
            this.lastMotionTime = Instant.now();
            this.detectionCount++;
        }
    }

    /**
     * @return timestamp of last detected motion (may be {@code null} if never detected)
     */
    public Instant getLastMotionTime() {
        return lastMotionTime;
    }

    /**
     * @param lastMotionTime timestamp of last detected motion
     */
    public void setLastMotionTime(Instant lastMotionTime) {
        this.lastMotionTime = lastMotionTime;
    }

    /**
     * @return number of times motion was detected
     */
    public int getDetectionCount() {
        return detectionCount;
    }

    /**
     * @param detectionCount number of detections
     */
    public void setDetectionCount(int detectionCount) {
        this.detectionCount = detectionCount;
    }

    /**
     * Returns the motion state as numeric value.
     *
     * @return 1 if motion is detected, otherwise 0
     */
    public int getMotionValue() {
        return motionDetected ? 1 : 0;
    }

    @Override
    public String toString() {
        return "MotionSensor{" +
                "label='" + getLabel() + '\'' +
                ", motionDetected=" + motionDetected +
                ", lastMotionTime=" + lastMotionTime +
                ", detectionCount=" + detectionCount +
                '}';
    }
}