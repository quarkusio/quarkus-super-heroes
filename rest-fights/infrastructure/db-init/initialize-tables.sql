\c fights_database superfight;

DROP TABLE IF EXISTS Hero;
DROP SEQUENCE IF EXISTS hibernate_sequence;

CREATE SEQUENCE hibernate_sequence START 1 INCREMENT 1;

CREATE TABLE Fight (
   id int8 NOT NULL,
   fightDate TIMESTAMP NOT NULL,
   loserLevel int4 NOT NULL,
   loserName VARCHAR(255),
   loserPicture VARCHAR(255),
   loserTeam VARCHAR(255),
   winnerLevel int4 NOT NULL,
   winnerName VARCHAR(255),
   winnerPicture VARCHAR(255),
   winnerTeam VARCHAR(255),
   PRIMARY KEY (id)
);
