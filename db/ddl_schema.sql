DROP TABLE IF EXISTS location_information CASCADE;
DROP TABLE IF EXISTS  address_information CASCADE;
DROP TABLE IF EXISTS home, user_information, home_user, room, device, sensor_type, sensor, actuator_type, actuator, sensor_reading, actuator_state CASCADE;

create table address_information (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    street VARCHAR(200),
    house_nr VARCHAR(8),
    post_code VARCHAR(15),
    city VARCHAR(200),
    country VARCHAR(50),
    longitude REAL,
    latitude REAL
);

/* honestly, unnecessary. Could be used if we stumble on issues with address-related look-ups
   Not deleting because the wrapper could be important.
   To be used in user_information as a field
create table addresses (
    id SERIAL PRIMARY KEY,
    info address_information NOT NULL
);
*/
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

create table room (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label VARCHAR(100),
    home_info INTEGER NOT NULL REFERENCES home (id) ON DELETE CASCADE,
    area numeric(5,2),
    UNIQUE(home_info, label)                               -- Each name is unique within each Home
);

create table device (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id INTEGER NOT NULL REFERENCES room(id) ON DELETE CASCADE,
    label VARCHAR(100)
);

create table sensor_type (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL, -- "Thermometer", "Humidity Sensor", "Light sensor", etc.
    unit VARCHAR(50),
    min_value NUMERIC(6,3) DEFAULT 0,
    max_value NUMERIC(6,3) DEFAULT 0
);

create table sensor
(
    device_id      INTEGER PRIMARY KEY REFERENCES device (id) ON DELETE CASCADE,
    sensor_type_id INTEGER NOT NULL REFERENCES sensor_type (id)
);

create table actuator_type
(
    id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    unit VARCHAR(20)
);

create table actuator
(
    device_id        INTEGER PRIMARY KEY REFERENCES device (id) ON DELETE CASCADE,
    actuator_type_id INTEGER NOT NULL REFERENCES actuator_type (id)
);

create table sensor_reading
(
    id        BIGSERIAL PRIMARY KEY,
    sensor_id INTEGER REFERENCES sensor (device_id) ON DELETE CASCADE,
    time      TIMESTAMP NOT NULL DEFAULT now(),
    value     REAL
);

create table actuator_state
(
    id          BIGSERIAL PRIMARY KEY,
    actuator_id INTEGER REFERENCES actuator (device_id) ON DELETE CASCADE,
    time        TIMESTAMP NOT NULL DEFAULT now(),
    state       jsonb
);