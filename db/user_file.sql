-- ============================================================
-- SmartHome Demo Seed (PostgreSQL)
-- Matches SensorReadingStatisticsRepository:
--   - date_trunc('hour', sr.time) bucketing
--   - half-open interval [fromInclusive, toExclusive)
--   - MIN/AVG/MAX across multiple sensors of same type in scope
--
-- Password hash for "test" (SHA256):
--   9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
-- ============================================================

BEGIN;

SELECT setseed(0.42);

-- ------------------------------------------------------------
-- 1) Addresses + Homes
-- ------------------------------------------------------------
WITH addr AS (
    INSERT INTO address_information (street, house_nr, post_code, city, country, longitude, latitude)
        VALUES
            ('Muldenstraße', '5', '4020', 'Linz', 'Austria', 14.308337, 48.278283), -- home 1 address
            ('Altenberger Straße',  '69',   '4040', 'Linz', 'Austria', 14.314274, 48.336251), -- home 2 address
            ('Am Wriezener Bahnhof',  '1',   '10243', 'Berlin', 'Germany', 13.440774, 52.511448)  -- extra user address
        RETURNING id
),
     home_ins AS (
         INSERT INTO home (floors, label, address_information)
             VALUES
                 (2, 'Gruppe2 SmartHome (Demo)', (SELECT id FROM addr ORDER BY id LIMIT 1)),
                 (1, 'Apartment (Demo)',        (SELECT id FROM addr ORDER BY id OFFSET 1 LIMIT 1))
             RETURNING id, label
     )
SELECT * FROM home_ins;

-- ------------------------------------------------------------
-- 2) Users + Home membership
-- ------------------------------------------------------------
WITH h AS (
    SELECT id, label FROM home
),
     a AS (
         SELECT id FROM address_information ORDER BY id
     ),
     u AS (
         INSERT INTO user_information (first_name, last_name, e_mail, password, home_info, address_info, avatar_path)
             VALUES
                 ('Alex',  'Owner',    'alex.owner@demo.local',
                  '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
                  (SELECT id FROM h WHERE label = 'Gruppe2 SmartHome (Demo)'),
                  (SELECT id FROM a ORDER BY id LIMIT 1),
                  '/avatars/alex.png'),

                 ('Riley', 'Resident', 'riley.resident@demo.local',
                  '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
                  (SELECT id FROM h WHERE label = 'Gruppe2 SmartHome (Demo)'),
                  (SELECT id FROM a ORDER BY id OFFSET 2 LIMIT 1),
                  '/avatars/riley.png'),

                 ('Sam',   'Guest',    'sam.guest@demo.local',
                  '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
                  (SELECT id FROM h WHERE label = 'Gruppe2 SmartHome (Demo)'),
                  NULL,
                  '/avatars/sam.png'),

                 ('Pat',   'Owner',    'pat.owner@demo.local',
                  '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
                  (SELECT id FROM h WHERE label = 'Apartment (Demo)'),
                  (SELECT id FROM a ORDER BY id OFFSET 1 LIMIT 1),
                  '/avatars/pat.png')
             RETURNING id, e_mail
     )
SELECT * FROM u;

INSERT INTO home_user (home_id, user_id, role)
SELECT h.id,
       u.id,
       CASE u.e_mail
           WHEN 'alex.owner@demo.local' THEN 'OWNER'::user_role
           WHEN 'riley.resident@demo.local' THEN 'RESIDENT'::user_role
           WHEN 'sam.guest@demo.local' THEN 'GUEST'::user_role
           WHEN 'pat.owner@demo.local' THEN 'OWNER'::user_role
           END
FROM home h
         JOIN user_information u
              ON (h.label = 'Gruppe2 SmartHome (Demo)' AND u.e_mail IN ('alex.owner@demo.local','riley.resident@demo.local','sam.guest@demo.local'))
                  OR (h.label = 'Apartment (Demo)' AND u.e_mail IN ('pat.owner@demo.local'));

-- ------------------------------------------------------------
-- 3) Rooms
-- ------------------------------------------------------------
WITH h AS (SELECT id, label FROM home)
INSERT INTO room (label, home_info, floor, length, width)
VALUES
    -- Home 1
    ('Living Room',   (SELECT id FROM h WHERE label='Gruppe2 SmartHome (Demo)'), 1, 6.5, 4.2),
    ('Kitchen',       (SELECT id FROM h WHERE label='Gruppe2 SmartHome (Demo)'), 1, 4.0, 3.2),
    ('Office',        (SELECT id FROM h WHERE label='Gruppe2 SmartHome (Demo)'), 1, 3.8, 3.0),
    ('Bedroom',       (SELECT id FROM h WHERE label='Gruppe2 SmartHome (Demo)'), 2, 4.8, 3.6),
    ('Bathroom',      (SELECT id FROM h WHERE label='Gruppe2 SmartHome (Demo)'), 2, 2.6, 2.2),
    ('Utility Room',  (SELECT id FROM h WHERE label='Gruppe2 SmartHome (Demo)'), 1, 2.4, 2.0),

    -- Home 2
    ('Studio',        (SELECT id FROM h WHERE label='Apartment (Demo)'),        1, 5.2, 4.0),
    ('Bath',          (SELECT id FROM h WHERE label='Apartment (Demo)'),        1, 2.4, 2.0);

-- ------------------------------------------------------------
-- 4) Devices + Sensors + Actuators
-- ------------------------------------------------------------
-- Home 1 devices
WITH r AS (SELECT id, label FROM room),
     d AS (
         INSERT INTO device (room_id, label)
             VALUES
                 -- Living Room
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR Thermometer'),
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR Humidity'),
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR CO2'),
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR Noise'),
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR Light Sensor'),
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR Motion'),
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR Heating'),
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR Ventilation'),
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR Smart Light'),
                 ((SELECT id FROM r WHERE label='Living Room'), 'LR Blinds'),

                 -- Kitchen
                 ((SELECT id FROM r WHERE label='Kitchen'), 'K Thermometer'),
                 ((SELECT id FROM r WHERE label='Kitchen'), 'K Humidity'),
                 ((SELECT id FROM r WHERE label='Kitchen'), 'K CO2'),
                 ((SELECT id FROM r WHERE label='Kitchen'), 'K Light Sensor'),
                 ((SELECT id FROM r WHERE label='Kitchen'), 'K Smart Plug'),

                 -- Office
                 ((SELECT id FROM r WHERE label='Office'), 'O Thermometer'),
                 ((SELECT id FROM r WHERE label='Office'), 'O Humidity'),
                 ((SELECT id FROM r WHERE label='Office'), 'O CO2'),
                 ((SELECT id FROM r WHERE label='Office'), 'O Noise'),
                 ((SELECT id FROM r WHERE label='Office'), 'O Light Sensor'),
                 ((SELECT id FROM r WHERE label='Office'), 'O Heating'),

                 -- Bedroom
                 ((SELECT id FROM r WHERE label='Bedroom'), 'B Thermometer'),
                 ((SELECT id FROM r WHERE label='Bedroom'), 'B Humidity'),
                 ((SELECT id FROM r WHERE label='Bedroom'), 'B CO2'),
                 ((SELECT id FROM r WHERE label='Bedroom'), 'B Motion'),
                 ((SELECT id FROM r WHERE label='Bedroom'), 'B Alarm System'),
                 ((SELECT id FROM r WHERE label='Bedroom'), 'B Blinds'),

                 -- Bathroom
                 ((SELECT id FROM r WHERE label='Bathroom'), 'BA Humidity'),
                 ((SELECT id FROM r WHERE label='Bathroom'), 'BA Ventilation'),

                 -- Utility Room
                 ((SELECT id FROM r WHERE label='Utility Room'), 'U Utility Meter'),
                 ((SELECT id FROM r WHERE label='Utility Room'), 'U Cat Sensor'),
                 ((SELECT id FROM r WHERE label='Utility Room'), 'U Cat Feeder')
             RETURNING id, label
     )
SELECT count(*) AS home1_devices_created FROM d;

-- Home 2 devices (apartment)
WITH r AS (SELECT id, label FROM room),
     d AS (
         INSERT INTO device (room_id, label)
             VALUES
                 ((SELECT id FROM r WHERE label='Studio'), 'S Thermometer'),
                 ((SELECT id FROM r WHERE label='Studio'), 'S Humidity'),
                 ((SELECT id FROM r WHERE label='Studio'), 'S CO2'),
                 ((SELECT id FROM r WHERE label='Studio'), 'S Light Sensor'),
                 ((SELECT id FROM r WHERE label='Studio'), 'S Smart Light'),
                 ((SELECT id FROM r WHERE label='Bath'),   'SB Humidity'),
                 ((SELECT id FROM r WHERE label='Bath'),   'SB Ventilation')
             RETURNING id, label
     )
SELECT count(*) AS home2_devices_created FROM d;

-- Sensors: map by device label patterns to device_type
INSERT INTO sensor (device_id, sensor_type_id)
SELECT d.id, dt.id
FROM device d
         JOIN device_type dt ON (
    (d.label ILIKE '%Thermometer%' AND dt.category='SENSOR' AND dt.label='Thermometer')
        OR (d.label ILIKE '%Humidity%'    AND dt.category='SENSOR' AND dt.label='HumiditySensor')
        OR (d.label ILIKE '%CO2%'         AND dt.category='SENSOR' AND dt.label='CO2Sensor')
        OR (d.label ILIKE '%Noise%'       AND dt.category='SENSOR' AND dt.label='NoiseSensor')
        OR (d.label ILIKE '%Light Sensor%'AND dt.category='SENSOR' AND dt.label='LightSensor')
        OR (d.label ILIKE '%Motion%'      AND dt.category='SENSOR' AND dt.label='MotionSensor')
        OR (d.label ILIKE '%Utility Meter%' AND dt.category='SENSOR' AND dt.label='UtilityMeter')
        OR (d.label ILIKE '%Cat Sensor%'  AND dt.category='SENSOR' AND dt.label='CatSensor')
    );

-- Actuators
INSERT INTO actuator (device_id, actuator_type_id)
SELECT d.id, dt.id
FROM device d
         JOIN device_type dt ON (
    (d.label ILIKE '%Heating%'      AND dt.category='ACTUATOR' AND dt.label='Heating')
        OR (d.label ILIKE '%Ventilation%'  AND dt.category='ACTUATOR' AND dt.label='Ventilation')
        OR (d.label ILIKE '%Smart Light%'  AND dt.category='ACTUATOR' AND dt.label='SmartLightActuator')
        OR (d.label ILIKE '%Blinds%'       AND dt.category='ACTUATOR' AND dt.label='Blinds')
        OR (d.label ILIKE '%Alarm System%' AND dt.category='ACTUATOR' AND dt.label='AlarmSystem')
        OR (d.label ILIKE '%Smart Plug%'   AND dt.category='ACTUATOR' AND dt.label='SmartPlug')
        OR (d.label ILIKE '%Cat Feeder%'   AND dt.category='ACTUATOR' AND dt.label='Cat Feeder')
    );

-- ------------------------------------------------------------
-- 5) Sensor readings (HOURLY for last 30 days)
--    This is the important part for your chart bucketing.
-- ------------------------------------------------------------
WITH all_sensors AS (
    SELECT
        s.device_id AS sensor_id,
        d.label     AS device_label,
        r.label     AS room_label,
        h.label     AS home_label
    FROM sensor s
             JOIN device d ON d.id = s.device_id
             JOIN room r   ON r.id = d.room_id
             JOIN home h   ON h.id = r.home_info
),
     ts AS (
         SELECT generate_series(
                        date_trunc('hour', now()) - interval '30 days',
                        date_trunc('hour', now()),
                        interval '1 hour'
                ) AS t
     )
INSERT INTO sensor_reading (sensor_id, time, value)
SELECT s.sensor_id,
       ts.t,
       CASE
           WHEN s.device_label ILIKE '%Thermometer%' THEN
               (21.5
                   + 2.2 * sin(extract(epoch from ts.t) / 86400.0 * 2*pi())
                   + CASE
                         WHEN s.room_label = 'Kitchen' THEN 0.9
                         WHEN s.room_label = 'Bedroom' THEN -0.7
                         WHEN s.room_label IN ('Bathroom','Bath') THEN 0.3
                         WHEN s.room_label = 'Office' THEN 0.2
                         ELSE 0
                    END
                   + (random() - 0.5) * 0.7)

           WHEN s.device_label ILIKE '%Humidity%' THEN
               (48
                   + 5.5 * sin(extract(epoch from ts.t) / 43200.0 * 2*pi())
                   + CASE
                         WHEN s.room_label IN ('Bathroom','Bath')
                             AND extract(hour from ts.t) IN (6,7,8,19,20,21)
                             THEN 18 + 12*random()
                         ELSE 0
                    END
                   + (random() - 0.5) * 3.5)

           WHEN s.device_label ILIKE '%CO2%' THEN
               CASE
                   WHEN s.room_label = 'Office' AND extract(hour from ts.t) BETWEEN 8 AND 18
                       THEN 850 + 650*random()
                   WHEN s.room_label = 'Bedroom' AND (extract(hour from ts.t) >= 22 OR extract(hour from ts.t) <= 7)
                       THEN 700 + 500*random()
                   WHEN s.room_label = 'Kitchen' AND extract(hour from ts.t) IN (7,8,12,13,18,19)
                       THEN 650 + 450*random()
                   ELSE 420 + 260*random()
                   END

           WHEN s.device_label ILIKE '%Noise%' THEN
               CASE
                   WHEN extract(hour from ts.t) BETWEEN 7 AND 21 THEN 32 + 26*random()
                   ELSE 22 + 9*random()
                   END

           WHEN s.device_label ILIKE '%Light Sensor%' THEN
               GREATEST(0,
                        CASE WHEN extract(hour from ts.t) BETWEEN 7 AND 18
                                 THEN 250 + 1100*random()
                             ELSE 2 + 12*random()
                            END
               )

           WHEN s.device_label ILIKE '%Motion%' THEN
               CASE
                   WHEN s.room_label = 'Living Room' AND extract(hour from ts.t) BETWEEN 17 AND 23 THEN (CASE WHEN random()<0.25 THEN 1 ELSE 0 END)
                   WHEN s.room_label = 'Office'      AND extract(hour from ts.t) BETWEEN 8  AND 18 THEN (CASE WHEN random()<0.20 THEN 1 ELSE 0 END)
                   ELSE (CASE WHEN random()<0.08 THEN 1 ELSE 0 END)
                   END

           WHEN s.device_label ILIKE '%Utility Meter%' THEN
               (1200
                   + (extract(epoch from ts.t - (now() - interval '30 days')) / 86400.0) * (4.0 + 2.5*random()))

           WHEN s.device_label ILIKE '%Cat Sensor%' THEN
               CASE WHEN random() < 0.06 THEN 1 ELSE 0 END

           ELSE
               10*random()
           END
FROM all_sensors s
         CROSS JOIN ts;

-- ------------------------------------------------------------
-- 6) Actuator states (HOURLY last 30d)
--    Not used by this chart, but nice for demo completeness.
-- ------------------------------------------------------------
WITH all_act AS (
    SELECT a.device_id AS actuator_id, d.label AS device_label, r.label AS room_label
    FROM actuator a
             JOIN device d ON d.id = a.device_id
             JOIN room r   ON r.id = d.room_id
),
     ts AS (
         SELECT generate_series(
                        date_trunc('hour', now()) - interval '30 days',
                        date_trunc('hour', now()),
                        interval '1 hour'
                ) AS t
     )
INSERT INTO actuator_state (actuator_id, time, state)
SELECT a.actuator_id,
       ts.t,
       CASE
           WHEN a.device_label ILIKE '%Heating%' THEN
               CASE
                   WHEN extract(month from ts.t) IN (11,12,1,2,3)
                       THEN (20 + floor(70*random()))::text
                   ELSE (0 + floor(30*random()))::text
                   END

           WHEN a.device_label ILIKE '%Smart Light%' THEN
               CASE WHEN extract(hour from ts.t) BETWEEN 18 AND 23 THEN 'ON' ELSE 'OFF' END

           WHEN a.device_label ILIKE '%Blinds%' THEN
               CASE WHEN extract(hour from ts.t) BETWEEN 7 AND 18
                        THEN (70 + floor(30*random()))::text
                    ELSE (0 + floor(20*random()))::text
                   END

           WHEN a.device_label ILIKE '%Ventilation%' THEN
               CASE WHEN a.room_label IN ('Bathroom','Bath')
                   AND extract(hour from ts.t) IN (6,7,8,19,20,21)
                        THEN 'ON'
                    ELSE (CASE WHEN random() < 0.25 THEN 'ON' ELSE 'OFF' END)
                   END

           WHEN a.device_label ILIKE '%Smart Plug%' THEN
               CASE WHEN random() < 0.50 THEN 'ON' ELSE 'OFF' END

           WHEN a.device_label ILIKE '%Alarm System%' THEN
               CASE WHEN (extract(hour from ts.t) >= 23 OR extract(hour from ts.t) <= 6) THEN 'ARMED' ELSE 'DISARMED' END

           WHEN a.device_label ILIKE '%Cat Feeder%' THEN
               CASE WHEN extract(hour from ts.t) IN (7,18) AND random() < 0.35 THEN 'DISPENSED' ELSE 'IDLE' END

           ELSE
               'IDLE'
           END
FROM all_act a
         CROSS JOIN ts;

-- ------------------------------------------------------------
-- 7) Rules + Invitations (for UI lists)
-- ------------------------------------------------------------
WITH h AS (SELECT id FROM home WHERE label='Gruppe2 SmartHome (Demo)')
INSERT INTO rule (home_id, name, enabled, priority, condition_json, action_json)
VALUES
    ((SELECT id FROM h), 'Ventilate on high CO2', TRUE, 10,
     '{"sensor":"CO2Sensor","room":"Office","op":">","value":1000}',
     '{"actuator":"Ventilation","room":"Living Room","state":"ON"}'),
    ((SELECT id FROM h), 'Lights on motion after sunset', TRUE, 5,
     '{"sensor":"MotionSensor","room":"Living Room","op":"==","value":1,"after":"sunset"}',
     '{"actuator":"SmartLightActuator","room":"Living Room","state":"ON"}'),
    ((SELECT id FROM h), 'Bathroom fan on humidity spike', TRUE, 7,
     '{"sensor":"HumiditySensor","room":"Bathroom","op":">","value":70}',
     '{"actuator":"Ventilation","room":"Bathroom","state":"ON"}');

WITH h AS (SELECT id FROM home WHERE label='Gruppe2 SmartHome (Demo)'),
     inviter AS (SELECT id FROM user_information WHERE e_mail='alex.owner@demo.local')
INSERT INTO home_invitation (home_id, inviter_user_id, invitee_email, invitation_status, invited_role, invited_at, responded_at)
VALUES
    ((SELECT id FROM h), (SELECT id FROM inviter), 'future.resident@demo.local', 'PENDING',  'RESIDENT', now() - interval '2 days', NULL),
    ((SELECT id FROM h), (SELECT id FROM inviter), 'declined.guest@demo.local', 'DECLINED', 'GUEST',    now() - interval '10 days', now() - interval '9 days');

COMMIT;