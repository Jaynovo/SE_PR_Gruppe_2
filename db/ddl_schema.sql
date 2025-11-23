create table location_information (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    longitude REAL,
    latitude REAL
);

create type address_information as (
    street VARCHAR(200),
    house_nr VARCHAR(8),
    plz VARCHAR(15),
    city VARCHAR(200),
    country VARCHAR(2)      -- Country Codes, created via backend/controller
);

/* honestly, unnecessary. Could be used if we stumble on issues with address-related look-ups
   Not deleting because the wrapper could be important.
   To be used in user_information as a field
create table addresses (
    id SERIAL PRIMARY KEY,
    info address_information NOT NULL
);
*/
create table home (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    floors SMALLINT,
    label VARCHAR(100),
    loc_info INTEGER NOT NULL REFERENCES location_information(id)    -- One house, one location. Many houses could have the same location (i.e. apartments)
);

create table user_information (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(25),
    last_name VARCHAR(25),
    e_mail VARCHAR(100) UNIQUE NOT NULL,
    address address_information,                                   -- see loc_info
    loc_info INTEGER NOT NULL REFERENCES location_information(id), -- is this even necessary?? Since any user only ever has one house
    home_info INTEGER NOT NULL REFERENCES home (id)               -- One User, one house. Many users, still one house
);

create table home_user (                                    -- define a house-user in case we want to use roles later
    home_id INTEGER NOT NULL REFERENCES home (id),
    user_id INTEGER NOT NULL REFERENCES user_information(id),
    role VARCHAR(20) DEFAULT 'MEMBER',
    PRIMARY KEY (home_id, user_id)
);



create table room (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label VARCHAR(100),
    home_info INTEGER NOT NULL REFERENCES home (id),
    area numeric(5,2),
    UNIQUE(home_info, label)                               -- Each name is unique within each house
);

create table device (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id INTEGER NOT NULL REFERENCES room(id),
    label VARCHAR(100)
);

create table sensor_type (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,                       -- "Thermometer", "Humidity Sensor", "Light sensor", etc.
    unit VARCHAR(50),
    min_value NUMERIC(3,3),
    max_value NUMERIC(3,3)
);

create table sensor (
    device_id INTEGER PRIMARY KEY REFERENCES device(id) ON DELETE CASCADE,
    sensor_type_id INTEGER NOT NULL REFERENCES sensor_type(id)
);

create table actuator_type (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    unit VARCHAR(20)
);

create table actuator (
    device_id INTEGER PRIMARY KEY REFERENCES device(id) ON DELETE CASCADE,
    actuator_type_id INTEGER NOT NULL REFERENCES actuator_type(id)
);

create table sensor_reading (
    id BIGSERIAL PRIMARY KEY,
    sensor_id INTEGER REFERENCES sensor(device_id) ON DELETE CASCADE,
    time TIMESTAMP NOT NULL DEFAULT now(),
    value REAL
);

create table actuator_state (
    id BIGSERIAL PRIMARY KEY ,
    actuator_id INTEGER REFERENCES actuator(device_id) ON DELETE CASCADE,
    time TIMESTAMP NOT NULL DEFAULT now(),
    state jsonb
);