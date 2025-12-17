INSERT INTO address_information(street, house_nr, post_code, city, country)
VALUES ('Stephansplatz', '3', '1010', 'Vienna', 'Austria');

INSERT INTO home(floors, label, address_information)
VALUES (3, 'Mustermanns home', 1);

INSERT INTO user_information(first_name, last_name, e_mail, password, home_info)
VALUES ('Max', 'Mustermann', 'max.mustermann@example.com',
        '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8',  1
       );

INSERT INTO room(label, home_info, area)
VALUES ('Living Room', 1, 24.6);

INSERT INTO room(label, home_info, area)
VALUES ('Bedroom', 1, 32);


INSERT INTO device_type (category, label, unit)
VALUES  ('SENSOR',   'Thermometer',      '°C'),
        ('SENSOR',   'Humidity Sensor',  '%'),
        ('ACTUATOR', 'Light Switch',     NULL),
        ('ACTUATOR', 'Heating',          '%'),
        ('SENSOR',   'CO2Sensor',   'ppm'),
        ('SENSOR',   'NoiseSensor', 'dB'),
        ('ACTUATOR', 'Ventilation', NULL),
        ('ACTUATOR', 'AlarmSystem',       NULL);

-- Living Room devices
INSERT INTO device(room_id, label)
VALUES (
           (SELECT id
            FROM room
            WHERE label = 'Living Room'
              AND home_info = (SELECT id FROM home WHERE label = 'Mustermanns home')),
           'Living Room Thermometer'
       ),
       (
           (SELECT id
            FROM room
            WHERE label = 'Living Room'
              AND home_info = (SELECT id FROM home WHERE label = 'Mustermanns home')),
           'Living Room Humidity Sensor'
       ),
       (
           (SELECT id FROM room
            WHERE label = 'Living Room'
              AND home_info = (SELECT id FROM home WHERE label = 'Mustermanns home')),
           'Living Room Ceiling Light'
       ),
       (
           (SELECT id FROM room
            WHERE label = 'Living Room'
              AND home_info = (SELECT id FROM home WHERE label = 'Mustermanns home')),
           'Living Room Radiator Valve'
       );

-- Bedroom devices
INSERT INTO device (room_id, label)
VALUES
    (
        (SELECT id FROM room
         WHERE label = 'Bedroom'
           AND home_info = (SELECT id FROM home WHERE label = 'Mustermanns home')),
        'Bedroom Thermometer'
    ),
    (
        (SELECT id FROM room
         WHERE label = 'Bedroom'
           AND home_info = (SELECT id FROM home WHERE label = 'Mustermanns home')),
        'Bedroom Ceiling Light'
    );


-- Living Room sensors
INSERT INTO sensor (device_id, sensor_type_id)
VALUES
    (
        (SELECT d.id
         FROM device d
                  JOIN room r ON d.room_id = r.id
         WHERE d.label = 'Living Room Thermometer'
           AND r.label = 'Living Room'),
        (SELECT id FROM device_type
         WHERE label = 'Thermometer'
           AND category = 'SENSOR')
    ),
    (
        (SELECT d.id
         FROM device d
                  JOIN room r ON d.room_id = r.id
         WHERE d.label = 'Living Room Humidity Sensor'
           AND r.label = 'Living Room'),
        (SELECT id FROM device_type
         WHERE label = 'Humidity Sensor'
           AND category = 'SENSOR')
    );

-- Bedroom sensors
INSERT INTO sensor (device_id, sensor_type_id)
VALUES
    (
        (SELECT d.id
         FROM device d
                  JOIN room r ON d.room_id = r.id
         WHERE d.label = 'Bedroom Thermometer'
           AND r.label = 'Bedroom'),
        (SELECT id FROM device_type
         WHERE label = 'Thermometer'
           AND category = 'SENSOR')
    );


-- Living Room actuators
-- Light Switch
INSERT INTO actuator (device_id, actuator_type_id)
SELECT d.id, dt.id
FROM device d
         JOIN room r ON d.room_id = r.id
         JOIN device_type dt
              ON dt.label = 'Light Switch'
                  AND dt.category = 'ACTUATOR'
WHERE d.label = 'Living Room Ceiling Light'
  AND r.label = 'Living Room';

-- Heating
INSERT INTO actuator (device_id, actuator_type_id)
SELECT d.id, dt.id
FROM device d
         JOIN room r ON d.room_id = r.id
         JOIN device_type dt
              ON dt.label = 'Heating'
                  AND dt.category = 'ACTUATOR'
WHERE d.label = 'Living Room Radiator Valve'
  AND r.label = 'Living Room';

-- Bedroom actuators
INSERT INTO actuator (device_id, actuator_type_id)
SELECT d.id, dt.id
FROM device d
         JOIN room r ON d.room_id = r.id
         JOIN device_type dt
              ON dt.label = 'Light Switch'
                  AND dt.category = 'ACTUATOR'
WHERE d.label = 'Bedroom Ceiling Light'
  AND r.label = 'Bedroom';

-- Nesting INSERTs is entirely possible. Keep in mind for actual app
-- INSERT INTO user_information (...)
-- VALUES (..., (INSERT INTO location_information(...) VALUES(...)));

/*
 Alternatively, we can assign FKs as variables beforehand:
 WITH   loc AS (INSERT INTO location_information(...) ... ),
        home AS (INSERT INTO home(...) ... )
 INSERT INTO user_information(...)
 VALUES (..., (SELECT id FROM loc), (SELECT id FROM home));
 */

-- For JDBC, see here:
-- https://stackoverflow.com/questions/16119257/retrieving-serial-id-from-batch-inserted-rows-in-postgresql/16119489#16119489