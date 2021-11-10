package io.quarkus.workshop.superheroes.statistics;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class Ranking {

    private final int max;

    private final Comparator<Score> comparator = Comparator.comparingInt(s -> -1 * s.score);

    private final LinkedList<Score> top = new LinkedList<>();

    public Ranking(int size) {
        max = size;
    }

    public Iterable<Score> onNewScore(Score score) {
        // Remove score if already present,
        top.removeIf(s -> s.name.equalsIgnoreCase(score.name));
        // Add the score
        top.add(score);
        // Sort
        top.sort(comparator);

        // Drop on overflow
        if (top.size() > max) {
            top.remove(top.getLast());
        }

        return Collections.unmodifiableList(top);
    }
}
