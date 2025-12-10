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


-- Generic Types of Thermometer and Sensor. Expand later with other sensors
INSERT INTO sensor_type(label, unit)
VALUES  ('Thermometer', '°C'),
        ('Humidity Sensor', '%');

-- Generic actuators, expand later
INSERT INTO actuator_type(label, unit)
VALUES ('Light Switch', NULL),
       ('Heating', '%');

-- Living Room devices
INSERT INTO device(room_id, label)
VALUES (
           (SELECT id
            FROM room
            WHERE label = 'Living Room' AND home_info = (SELECT id FROM home WHERE label = 'Mustermanns home')),
           'Living Room Thermometer'
       ),
       (
           (SELECT id
            FROM room
            WHERE label = 'Living Room' AND home_info = (SELECT id FROM home WHERE label = 'Mustermanns home')),
           'Living Room Humidity Sensor'
       ),
       (
           (SELECT id FROM room
            WHERE label = 'Living Room' AND home_info = (SELECT id FROM home WHERE label = 'Mustermanns home')),
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
        (SELECT id FROM sensor_type WHERE label = 'Thermometer')
    ),
    (
        (SELECT d.id
         FROM device d
                  JOIN room r ON d.room_id = r.id
         WHERE d.label = 'Living Room Humidity Sensor'
           AND r.label = 'Living Room'),
        (SELECT id FROM sensor_type WHERE label = 'Humidity Sensor')
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
        (SELECT id FROM sensor_type WHERE label = 'Thermometer')
    );

-- Living Room actuators
-- Light Switch
INSERT INTO actuator (device_id, actuator_type_id)
SELECT d.id, at.id
FROM device d
         JOIN room r ON d.room_id = r.id
         JOIN actuator_type at ON at.label = 'Light Switch'
WHERE d.label = 'Living Room Ceiling Light'
  AND r.label = 'Living Room';

-- Heating
INSERT INTO actuator (device_id, actuator_type_id)
SELECT d.id, at.id
FROM device d
         JOIN room r ON d.room_id = r.id
         JOIN actuator_type at ON at.label = 'Heating'
WHERE d.label = 'Living Room Radiator Valve'
  AND r.label = 'Living Room';

-- Bedroom actuators
INSERT INTO actuator (device_id, actuator_type_id)
SELECT d.id, at.id
FROM device d
         JOIN room r ON d.room_id = r.id
         JOIN actuator_type at ON at.label = 'Light Switch'
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