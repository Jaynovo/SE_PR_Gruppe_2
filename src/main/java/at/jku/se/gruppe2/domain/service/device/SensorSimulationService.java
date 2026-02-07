package at.jku.se.gruppe2.domain.service.device;

import at.jku.se.gruppe2.domain.model.device.sensor.*;
import at.jku.se.gruppe2.infrastructure.config.LocalSecrets;
import at.jku.se.gruppe2.application.integration.RoboflowWorkflowService;

import java.util.*;
import java.util.concurrent.*;

/**
 * Simuliert Sensorwerte (CO₂ ppm und Geräusch dB) in festen Intervallen.
 * Schreibt die Werte direkt in die Sensor-Objekte (setValue()).
 */
public class SensorSimulationService {

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final Random rnd = new Random();

    private final java.util.concurrent.ExecutorService roboflowExec =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    private static final List<String> TEST_IMAGES = List.of(
            //waschbär
            "https://i.imgur.com/t6gGR4V.jpg",

            // cats
            "https://i.imgur.com/BxKNfWu.jpeg",
            "https://i.imgur.com/4XynTW7.jpeg",

            // no cats
            "https://i.imgur.com/IuqtS85.jpeg",
            "https://i.imgur.com/tgfa41l.jpeg",
            "https://i.imgur.com/dm871pH.jpeg"

    );
    // pro Raum: welcher Index ist als nächstes dran?
    private final Map<Integer, Integer> catImageIndexByRoom = new ConcurrentHashMap<>();

    // nur alle 10s einen Roboflow Call
    private final Map<Integer, Long> catLastCallMs = new ConcurrentHashMap<>();
    private static final long CAT_CALL_INTERVAL_MS = 10_000;

    // pro Raum je ein Sensor
    //TODO: weitere Sensoren hinzufügen
    private final Map<Integer, CO2Sensor> co2ByRoom = new ConcurrentHashMap<>();
    private final Map<Integer, NoiseSensor> noiseByRoom = new ConcurrentHashMap<>();
    private final Map<Integer, Thermometer> thermoByRoom = new ConcurrentHashMap<>();
    private final Map<Integer, HumiditySensor> humidityByRoom = new ConcurrentHashMap<>();

    // "Grundniveau" pro Raum (damit es nicht komplett zufällig springt)
    //TODO: weitere Sensoren hinzufügen
    private final Map<Integer, Double> co2Baseline = new ConcurrentHashMap<>();
    private final Map<Integer, Double> noiseBaseline = new ConcurrentHashMap<>();
    private final Map<Integer, CatSensor> catByRoom = new ConcurrentHashMap<>();
    private final Map<Integer, Double> thermoBaseline = new ConcurrentHashMap<>();
    private final Map<Integer, Double> humidityBaseline = new ConcurrentHashMap<>();

    private volatile boolean running = false;

    private final RoboflowWorkflowService roboflow =
            new RoboflowWorkflowService(LocalSecrets.ROBOFLOW_API_KEY, 0.75);


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
        if (sensor instanceof CatSensor cat) {
            catByRoom.put(roomId, cat);
            catImageIndexByRoom.putIfAbsent(roomId, 0);
        }
        if (sensor instanceof Thermometer t) {
            thermoByRoom.put(roomId, t);
            thermoBaseline.putIfAbsent(roomId, 21.0); // z.B. Start bei 21°C
        }
        if (sensor instanceof HumiditySensor h) {
            humidityByRoom.put(roomId, h);
            humidityBaseline.putIfAbsent(roomId, 45.0); // Start: 45%
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
        roboflowExec.shutdownNow();
    }

    public void clearRoom(int roomId) {
        co2ByRoom.remove(roomId);
        noiseByRoom.remove(roomId);
        co2Baseline.remove(roomId);
        noiseBaseline.remove(roomId);
        catByRoom.remove(roomId);
        catImageIndexByRoom.remove(roomId);
        thermoByRoom.remove(roomId);
        thermoBaseline.remove(roomId);
        humidityByRoom.remove(roomId);
        humidityBaseline.remove(roomId);
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
        for (Integer roomId : thermoByRoom.keySet()) {
            simulateThermometer(roomId);
        }
        for (Integer roomId : humidityByRoom.keySet()) {
            simulateHumidity(roomId);
        }
        //TODO: weitere Sensoren hinzufügen
        for (Integer roomId : catByRoom.keySet()) {
            simulateCat(roomId);
        }
    }

    private void simulateHumidity(Integer roomId) {
        HumiditySensor sensor = humidityByRoom.get(roomId);
        if (sensor == null) return;

        double base = humidityBaseline.getOrDefault(roomId, 45.0);

        // langsamer Drift
        double target = 48.0;
        base += (target - base) * 0.05;

        // leichtes Rauschen
        base += rnd.nextGaussian() * 0.6;

        // seltenes Event: Feuchtigkeit steigt (Duschen/Kochen)
        if (rnd.nextDouble() < 0.06) { // 6% pro Tick
            base += 4.0 + rnd.nextDouble() * 12.0; // +4..+16%
        }

        // seltenes Event: Lüften/trockene Luft
        if (rnd.nextDouble() < 0.04) { // 4% pro Tick
            base -= 3.0 + rnd.nextDouble() * 10.0; // -3..-13%
        }

        // baseline clamp
        base = clamp(base, 20, 80);
        humidityBaseline.put(roomId, base);

        // finaler Messwert
        double value = clamp(base + rnd.nextGaussian() * 0.3, 10, 95);

        sensor.setValue(value);
    }

    private void simulateCat(int roomId) {
        CatSensor sensor = catByRoom.get(roomId);
        if (sensor == null) return;

        long now = System.currentTimeMillis();
        long last = catLastCallMs.getOrDefault(roomId, 0L);
        if (now - last < CAT_CALL_INTERVAL_MS) return;
        catLastCallMs.put(roomId, now);

        int idx = catImageIndexByRoom.getOrDefault(roomId, 0);
        String imageUrl = TEST_IMAGES.get(idx);

        // Index weiterdrehen
        idx = (idx + 1) % TEST_IMAGES.size();
        catImageIndexByRoom.put(roomId, idx);

        roboflowExec.submit(() -> {
            try {
                RoboflowWorkflowService.DetectionResult result =
                        roboflow.detectCatFromImageUrlAsBase64(imageUrl);

                sensor.updateDetection(result.confidence(), imageUrl);
            } catch (Exception e) {
                sensor.updateDetection(0.0, imageUrl);
                System.err.println("Roboflow failed for: " +imageUrl + " -> " + e.getMessage());
            }
        });
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

    private void simulateThermometer(int roomId) {
        Thermometer sensor = thermoByRoom.get(roomId);
        if (sensor == null) return;

        double base = thermoBaseline.getOrDefault(roomId, 21.0);

        // kleine Drift
        base += rnd.nextGaussian() * 0.15; // ca ±0.15°C

        // seltenes Event (Fenster offen / Sonne / Heizung an)
        if (rnd.nextDouble() < 0.05) {
            base += (rnd.nextBoolean() ? 1 : -1) * (0.5 + rnd.nextDouble() * 1.5); // ±0.5..2.0°C
        }

        base = clamp(base, 15, 30);
        thermoBaseline.put(roomId, base);

        // finaler Messwert mit kleinem Rauschen
        double value = clamp(base + rnd.nextGaussian() * 0.05, 10, 35);
        sensor.setValue(value);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}