DROP TABLE IF EXISTS locations;
DROP SEQUENCE IF EXISTS locations_SEQ;

CREATE SEQUENCE locations_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE locations (
	id bigint NOT NULL,
	description VARCHAR(5000),
	name VARCHAR(50) NOT NULL,
	picture VARCHAR(255),
	type ENUM ('CITY','PLANET') NOT NULL,
	PRIMARY KEY (id)
) engine=InnoDB;

ALTER TABLE locations ADD CONSTRAINT UK_name UNIQUE (name);

INSERT INTO locations(id, name, description, picture, type)
VALUES (next value for locations_SEQ, 'Gotham City', 'An American city rife with corruption and crime, the home of its iconic protector Batman.', 'https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/gotham_city.jpg', 'CITY');
INSERT INTO locations(id, name, description, picture, type)
VALUES (next value for locations_SEQ, 'Earth', 'Earth, our home planet, is the only place we know of so far that is inhabited by living things.', 'https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/earth.jpg', 'PLANET');
INSERT INTO locations(id, name, description, picture, type)
VALUES (next value for locations_SEQ, 'Krypton', 'An ancient world, Krypton was home to advanced civilization known as Kryptonians. In one reality they possessed superhuman strength among other abilities far beyond those of Earth humans. In the majority of realities, these powers would not appear unless the Kryptonian was offworld or under the light of an alien sun.', 'https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/krypton.jpg', 'PLANET');
INSERT INTO locations(id, name, description, picture, type)
VALUES (next value for locations_SEQ, 'Metropolis', 'Metropolis is depicted as being one of the largest and wealthiest cities in the world, having a population of 11 million citizens. In addition to Superman, the city has also been home to other superheroes, such as Booster Gold and Black Lightning.', 'https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/metropolis.jpg', 'CITY');
