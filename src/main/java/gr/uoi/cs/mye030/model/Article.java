package gr.uoi.cs.mye030.model;

import java.time.LocalDate;

public record Article(
        int id,
        ArticleType type,
        String title,
        String acronym,
        Integer journalId,
        Integer conferenceId,
        LocalDate datePub,
        String publisher
) {}
