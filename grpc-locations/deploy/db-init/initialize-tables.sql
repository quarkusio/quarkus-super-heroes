DROP TABLE IF EXISTS locations;
DROP SEQUENCE IF EXISTS locations_SEQ;

CREATE SEQUENCE locations_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE locations (
	id bigint NOT NULL,
	description VARCHAR(255),
	name VARCHAR(50) NOT NULL,
	picture VARCHAR(255),
	type ENUM ('CITY','PLANET') NOT NULL,
	PRIMARY KEY (id)
) engine=InnoDB;

ALTER TABLE locations ADD CONSTRAINT UK_name UNIQUE (name);

INSERT INTO locations(id, name, description, picture, type) VALUES (next value for locations_SEQ, 'Gotham City', 'Dark city where Batman lives', 'gotham_city.png', 'CITY');
