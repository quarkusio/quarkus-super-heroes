package io.quarkus.workshop.superheroes.statistics;

class TeamStats {

    private int villains = 0;
    private int heroes = 0;

    double add(Fight result) {
        if (result.winnerTeam.equalsIgnoreCase("heroes")) {
            heroes = heroes + 1;
        } else {
            villains = villains + 1;
        }
        return ((double) heroes / (heroes + villains));
    }

}
