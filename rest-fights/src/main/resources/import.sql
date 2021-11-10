INSERT INTO fight(id, fightDate, winnerName, winnerLevel, winnerPicture, loserName, loserLevel, loserPicture, winnerTeam, loserTeam)
VALUES (nextval('hibernate_sequence'), current_timestamp, 'Chewbacca', 5, 'https://www.superherodb.com/pictures2/portraits/10/050/10466.jpg', 'Buuccolo', 3, 'https://www.superherodb.com/pictures2/portraits/11/050/15355.jpg', 'heroes', 'villains');
INSERT INTO fight(id, fightDate, winnerName, winnerLevel, winnerPicture, loserName, loserLevel, loserPicture, winnerTeam ,loserTeam)
VALUES (nextval('hibernate_sequence'), current_timestamp, 'Galadriel', 10, 'https://www.superherodb.com/pictures2/portraits/11/050/11796.jpg', 'Darth Vader', 8, 'https://www.superherodb.com/pictures2/portraits/10/050/10444.jpg', 'heroes', 'villains');
INSERT INTO fight(id, fightDate, winnerName, winnerLevel, winnerPicture, loserName, loserLevel, loserPicture, winnerTeam ,loserTeam)
VALUES (nextval('hibernate_sequence'), current_timestamp, 'Annihilus', 23, 'https://www.superherodb.com/pictures2/portraits/10/050/1307.jpg', 'Shikamaru', 1, 'https://www.superherodb.com/pictures2/portraits/10/050/11742.jpg', 'villains', 'heroes');
