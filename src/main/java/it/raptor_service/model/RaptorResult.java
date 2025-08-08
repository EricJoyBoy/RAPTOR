package it.raptor_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RaptorResult {
    private Map<Integer, LevelResult> levelResults;
    private List<String> allTexts;
}