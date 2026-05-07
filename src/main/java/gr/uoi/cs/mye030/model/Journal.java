package gr.uoi.cs.mye030.model;

public record Journal(
        int id,
        int rank,
        String title,
        String acronym,
        String country,
        String bestSubjectArea,
        int totalDocs,
        int totalRefs
) {}
