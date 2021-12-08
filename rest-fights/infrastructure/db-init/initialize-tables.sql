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

INSERT INTO fight(id, fightDate, winnerName, winnerLevel, winnerPicture, loserName, loserLevel, loserPicture, winnerTeam, loserTeam)
VALUES (nextval('hibernate_sequence'), current_timestamp, 'Chewbacca', 5, 'https://www.superherodb.com/pictures2/portraits/10/050/10466.jpg', 'Buuccolo', 3, 'https://www.superherodb.com/pictures2/portraits/11/050/15355.jpg', 'heroes', 'villains');
INSERT INTO fight(id, fightDate, winnerName, winnerLevel, winnerPicture, loserName, loserLevel, loserPicture, winnerTeam ,loserTeam)
VALUES (nextval('hibernate_sequence'), current_timestamp, 'Galadriel', 10, 'https://www.superherodb.com/pictures2/portraits/11/050/11796.jpg', 'Darth Vader', 8, 'https://www.superherodb.com/pictures2/portraits/10/050/10444.jpg', 'heroes', 'villains');
INSERT INTO fight(id, fightDate, winnerName, winnerLevel, winnerPicture, loserName, loserLevel, loserPicture, winnerTeam ,loserTeam)
VALUES (nextval('hibernate_sequence'), current_timestamp, 'Annihilus', 23, 'https://www.superherodb.com/pictures2/portraits/10/050/1307.jpg', 'Shikamaru', 1, 'https://www.superherodb.com/pictures2/portraits/10/050/11742.jpg', 'villains', 'heroes');
