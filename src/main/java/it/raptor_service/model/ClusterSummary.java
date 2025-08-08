package it.raptor_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusterSummary {
    private int id;
    private int level;
    private String summary;
    private List<Integer> textIds;
}