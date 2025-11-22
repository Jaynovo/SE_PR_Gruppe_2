create table location_information (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    longitude REAL,
    latitude REAL
);

create type address_information as (
    street VARCHAR(50),
    house_nr smallint,
    plz smallint,
    city VARCHAR(20),
    country VARCHAR(25)
);

/* honestly, unnecessary. Could be used if we stumble on issues with address-related look-ups
   Not deleting because the wrapper could be important.
   To be used in user_information as a field
create table addresses (
    id SERIAL PRIMARY KEY,
    info address_information NOT NULL
);
*/

create table user_information (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(25),
    last_name VARCHAR(25),
    e_mail VARCHAR(100) UNIQUE NOT NULL,
    address address_information,
    loc_info INTEGER REFERENCES location_information(id)
);
