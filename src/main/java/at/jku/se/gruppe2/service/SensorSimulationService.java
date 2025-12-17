package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.model.CO2Sensor;
import at.jku.se.gruppe2.model.NoiseSensor;
import at.jku.se.gruppe2.model.Sensor;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simuliert Sensorwerte (CO2 ppm und Geräusch dB) in festen Intervallen.
 * Schreibt die Werte direkt in die Sensor-Objekte (setValue()).
 */
public class SensorSimulationService {

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final Random rnd = new Random();

    // pro Raum je ein Sensor
    //TODO: weitere Sensoren hinzufügen
    private final Map<Integer, CO2Sensor> co2ByRoom = new ConcurrentHashMap<>();
    private final Map<Integer, NoiseSensor> noiseByRoom = new ConcurrentHashMap<>();

    // "Grundniveau" pro Raum (damit es nicht komplett zufällig springt)
    //TODO: weitere Sensoren hinzufügen
    private final Map<Integer, Double> co2Baseline = new ConcurrentHashMap<>();
    private final Map<Integer, Double> noiseBaseline = new ConcurrentHashMap<>();

    private volatile boolean running = false;

    /**
     * Registriert Sensoren eines Raumes für die Simulation.
     */
    public void registerSensor(int roomId, Sensor sensor) {

        if (sensor instanceof CO2Sensor co2) {
            co2ByRoom.put(roomId, co2);
            co2Baseline.putIfAbsent(roomId, 650.0);
        }

        if (sensor instanceof NoiseSensor noise) {
            noiseByRoom.put(roomId, noise);
            noiseBaseline.putIfAbsent(roomId, 35.0);
        }
        //TODO: weitere Sensoren hinzufügen
    }

    /**
     * Startet die Simulation.
     *
     * @param everySeconds Intervall in Sekunden (z.B. 2)
     */
    public void start(int everySeconds) {
        if (everySeconds <= 0) throw new IllegalArgumentException("everySeconds must be > 0");
        if (running) return;
        running = true;
        exec.scheduleAtFixedRate(this::tick, 0, everySeconds, TimeUnit.SECONDS);
    }

    /**
     * Stoppt die Simulation.
     */
    public void stop() {
        running = false;
        exec.shutdownNow();
    }

    private void tick() {
        if (!running) return;

        // CO2-Sensoren simulieren
        for (Integer roomId : co2ByRoom.keySet()) {
            simulateCo2(roomId);
        }

        // Geräuschsensoren simulieren
        for (Integer roomId : noiseByRoom.keySet()) {
            simulateNoise(roomId);
        }
        //TODO: weitere Sensoren hinzufügen
    }


    private void simulateCo2(int roomId) {
        CO2Sensor sensor = co2ByRoom.get(roomId);
        if (sensor == null) return;

        double base = co2Baseline.getOrDefault(roomId, 650.0);

        // kleine Drift
        base += rnd.nextGaussian() * 20.0;

        // seltenes "Event": viele Leute im Raum / schlechte Luft
        if (rnd.nextDouble() < 0.08) { // 8% pro Tick
            base += 200 + rnd.nextDouble() * 600; // +200..+800
        }

        // clamp realistisch: 400..2500 ppm
        base = clamp(base, 400, 2500);

        co2Baseline.put(roomId, base);

        // finaler Messwert mit leichtem Rauschen
        double value = clamp(base + rnd.nextGaussian() * 15.0, 400, 3000);

        sensor.setValue(value);
    }

    private void simulateNoise(int roomId) {
        NoiseSensor sensor = noiseByRoom.get(roomId);
        if (sensor == null) return;

        double base = noiseBaseline.getOrDefault(roomId, 35.0);

        // kleine Drift
        base += rnd.nextGaussian() * 2.0;

        // seltenes "Lärm-Event": Tür knallt / laute Musik / Streit
        if (rnd.nextDouble() < 0.10) { // 10% pro Tick
            base += 20 + rnd.nextDouble() * 50; // +20..+70 dB extra
        }

        // clamp baseline: 25..80
        base = clamp(base, 25, 80);

        noiseBaseline.put(roomId, base);

        // Messwert: baseline + event noise
        double value = clamp(base + Math.abs(rnd.nextGaussian() * 3.0), 20, 120);

        sensor.setValue(value);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}