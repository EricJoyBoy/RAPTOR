package it.raptor_service.model;

public class TextEmbedding {
    private final int id;
    private final String text;
    private final float[] embedding;

    public TextEmbedding(int id, String text, float[] embedding) {
        this.id = id;
        this.text = text;
        this.embedding = embedding;
    }

    public int getId() { return id; }
    public String getText() { return text; }
    public float[] getEmbedding() { return embedding; }
}