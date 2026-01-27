package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.model.Room;
import at.jku.se.gruppe2.model.actuator.HeatingActuator;
import at.jku.se.gruppe2.model.sensor.Thermometer;
import at.jku.se.gruppe2.service.actuator.ActuatorService;

import java.util.Optional;

public class ClimateControlService {

    private final ActuatorService actuatorService = new ActuatorService();

    public void evaluate(Room room) {
        Optional<Thermometer> thermometer = getThermometer(room);
        Optional<HeatingActuator> heater = getHeating(room);

        if (thermometer.isEmpty() || heater.isEmpty()) return;

        double temperature = thermometer.get().getValue();
        double min = room.getMinTemperature();
        double max = room.getMaxTemperature();

        if (temperature < min && !heater.get().isOn()) {
            heater.get().turnOn();
            actuatorService.setState(heater.get().getId(), heater.get().getState());
        }

        if (temperature > max && heater.get().isOn()) {
            heater.get().turnOff();
            actuatorService.setState(heater.get().getId(), heater.get().getState());
        }
    }

    private Optional<HeatingActuator> getHeating(Room room) {
        return room.getDevices().stream()
                .filter(HeatingActuator.class::isInstance)
                .map(HeatingActuator.class::cast)
                .findFirst();
    }

    private Optional<Thermometer> getThermometer(Room room) {
        return room.getDevices().stream()
                .filter(Thermometer.class::isInstance)
                .map(Thermometer.class::cast)
                .findFirst();
    }

}
