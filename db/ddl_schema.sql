DROP TABLE IF EXISTS location_information CASCADE;
DROP TABLE IF EXISTS address_information CASCADE;
DROP TYPE IF EXISTS device_category CASCADE;
DROP TABLE IF EXISTS home, user_information, home_user, room, device, sensor_type, sensor, actuator_type, actuator, sensor_reading, actuator_state CASCADE;
DROP TABLE IF EXISTS device_type, home_invitation CASCADE;

create table address_information (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    street VARCHAR(200),
    house_nr VARCHAR(8),
    post_code VARCHAR(15),
    city VARCHAR(200),
    country VARCHAR(50),
    longitude DOUBLE PRECISION,
    latitude DOUBLE PRECISION
);

create table home
(
    id                  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    floors              SMALLINT     DEFAULT 0,
    label               VARCHAR(100) DEFAULT 'Home',
    address_information INTEGER REFERENCES address_information (id) -- One house, one location. Many houses could have the same location (i.e. apartments)
);

create table user_information
(
    id         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(25),
    last_name VARCHAR(25),
    e_mail VARCHAR(100) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    home_info INTEGER REFERENCES home (id) ON DELETE SET NULL                         -- One User, one house. Many users, still one house. May be NULL if User doesn't have a home :(
);

create table home_user
( -- define a house-user in case we want to use roles later
    home_id INTEGER NOT NULL REFERENCES home (id),
    user_id INTEGER NOT NULL REFERENCES user_information (id),
    role    VARCHAR(20) DEFAULT 'MEMBER',
    PRIMARY KEY (home_id, user_id)
);

create table room
(
    id        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label     VARCHAR(100) NOT NULL,
    home_info INTEGER      NOT NULL
        REFERENCES home (id) ON DELETE CASCADE,

    floor     INTEGER NOT NULL CHECK (floor > 0),
    length    DOUBLE PRECISION CHECK (length > 0),
    width     DOUBLE PRECISION CHECK (width > 0),

    UNIQUE (home_info, label)
);

create type device_category as enum ('SENSOR', 'ACTUATOR');

create table device_type (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category device_category NOT NULL,
    label VARCHAR(50) NOT NULL,
    unit VARCHAR(50),
    UNIQUE (category, label)
);

create table device (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id INTEGER NOT NULL REFERENCES room(id) ON DELETE CASCADE,
    label VARCHAR(100)
);

create table sensor
(
    device_id      INTEGER PRIMARY KEY REFERENCES device (id) ON DELETE CASCADE,
    sensor_type_id INTEGER NOT NULL REFERENCES device_type (id)
);

create table actuator
(
    device_id        INTEGER PRIMARY KEY REFERENCES device (id) ON DELETE CASCADE,
    actuator_type_id INTEGER NOT NULL REFERENCES device_type (id)
);

create table sensor_reading
(
    id        BIGSERIAL PRIMARY KEY,
    sensor_id INTEGER REFERENCES sensor (device_id) ON DELETE CASCADE,
    time      TIMESTAMP NOT NULL DEFAULT now(),
    value     DOUBLE PRECISION
);

create table actuator_state
(
    id          BIGSERIAL PRIMARY KEY,
    actuator_id INTEGER REFERENCES actuator (device_id) ON DELETE CASCADE,
    time        TIMESTAMP NOT NULL DEFAULT now(),
    state       VARCHAR(50)
);

-- Table to store home invitations
CREATE TABLE home_invitation (
                                 id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 home_id INTEGER NOT NULL REFERENCES home(id) ON DELETE CASCADE,
                                 inviter_user_id INTEGER NOT NULL REFERENCES user_information(id) ON DELETE CASCADE,
                                 invitee_email VARCHAR(100) NOT NULL,
                                 invitation_status VARCHAR(20) DEFAULT 'PENDING' CHECK (invitation_status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
                                 invited_at TIMESTAMP NOT NULL DEFAULT now(),
                                 responded_at TIMESTAMP,
                                 UNIQUE (home_id, invitee_email)
);

CREATE INDEX idx_home_invitation_email ON home_invitation(invitee_email);
CREATE INDEX idx_home_invitation_status ON home_invitation(invitation_status);

-- ADD Permanent Device below --

INSERT INTO device_type (category, label, unit)
VALUES  ('SENSOR',   'Thermometer',      '°C'),
        ('SENSOR',   'HumiditySensor',  '%'),
        ('ACTUATOR', 'Light Switch',     NULL), -- TODO is this even necessary??
        ('ACTUATOR', 'Heating',          '%'),
        ('SENSOR',   'CO2Sensor',   'ppm'),
        ('SENSOR',   'NoiseSensor', 'dB'),
        ('ACTUATOR', 'Ventilation', NULL),
        ('ACTUATOR', 'AlarmSystem',       NULL),
        ('SENSOR', 'LightSensor', 'lx'),
        ('ACTUATOR', 'SmartLightActuator', NULL),
        ('ACTUATOR', 'Blinds', NULL);