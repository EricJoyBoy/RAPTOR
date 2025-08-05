package it.raptor_service.model;

import java.util.List;
import java.util.Map;


public class RaptorResult {
    private final Map<Integer, LevelResult> levelResults;
    private final List<String> allTexts;

    public RaptorResult(Map<Integer, LevelResult> levelResults, List<String> allTexts) {
        this.levelResults = levelResults;
        this.allTexts = allTexts;
    }

    public Map<Integer, LevelResult> getLevelResults() { return levelResults; }
    public List<String> getAllTexts() { return allTexts; }
}