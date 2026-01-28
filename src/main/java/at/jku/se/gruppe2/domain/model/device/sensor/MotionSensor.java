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

    public MotionSensor() {
        super();
        this.motionDetected = false;
        this.detectionCount = 0;
    }

    public boolean isMotionDetected() {
        return motionDetected;
    }

    public void setMotionDetected(boolean motionDetected) {
        this.motionDetected = motionDetected;
        if (motionDetected) {
            this.lastMotionTime = Instant.now();
            this.detectionCount++;
        }
    }

    public Instant getLastMotionTime() {
        return lastMotionTime;
    }

    public void setLastMotionTime(Instant lastMotionTime) {
        this.lastMotionTime = lastMotionTime;
    }

    public int getDetectionCount() {
        return detectionCount;
    }

    public void setDetectionCount(int detectionCount) {
        this.detectionCount = detectionCount;
    }

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