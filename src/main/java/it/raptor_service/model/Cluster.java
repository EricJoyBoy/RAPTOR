package it.raptor_service.model;

import java.util.List;

public class Cluster {
    private final int id;
    private final List<String> texts;
    private final List<Integer> textIds;

    public Cluster(int id, List<String> texts, List<Integer> textIds) {
        this.id = id;
        this.texts = texts;
        this.textIds = textIds;
    }

    public int getId() { return id; }
    public List<String> getTexts() { return texts; }
    public List<Integer> getTextIds() { return textIds; }
}