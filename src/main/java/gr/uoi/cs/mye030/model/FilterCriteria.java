package gr.uoi.cs.mye030.model;

import java.util.Set;

public record FilterCriteria(
        ArticleType type,
        Integer yearFrom,
        Integer yearTo,
        Set<String> countries,
        Set<String> ranks,
        String publisher,
        Integer topN
) {
    public static FilterCriteria empty() {
        return new FilterCriteria(null, null, null, Set.of(), Set.of(), null, null);
    }
}
