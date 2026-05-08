package gr.uoi.cs.mye030.service;

import gr.uoi.cs.mye030.model.Author;

import java.util.List;

public final class ChartData {

    private ChartData() {}

    public record YearCount(int year, long count) {}

    public record MultiYearSeries(String name, List<YearCount> points) {}

    public record EntityBarStats(int id, String name, long totalArticles,
                                 double avgArticlesPerYear, double avgAuthorsPerYear) {}

    public record PublisherJournalQuarters(String publisher, long total,
                                           long q1, long q2, long q3, long q4) {}

    public record AuthorListRow(Author author, long articleCount) {}

    public record AuthorProfileStats(Integer firstYear, Integer lastYear, long totalArticles) {
        public int yearSpan() {
            if (firstYear == null || lastYear == null) return 0;
            return lastYear - firstYear + 1;
        }

        public double avgArticlesPerYear() {
            int s = yearSpan();
            return s == 0 ? 0.0 : (double) totalArticles / s;
        }
    }

    public record CategoryCount(String category, long count) {}

    public record CountryCount(String country, long count) {}

    public record YearlyAuthorCounts(int year, long distinctAuthors, long totalAuthors) {}

    public record YearProfileStats(
            long totalArticles,
            long distinctJournals,
            long distinctConferences,
            long distinctAuthors,
            long totalAuthors
    ) {}

    public record YearPublication(int articleId, String title, String authors) {}

    public record ProfileStats(
            Integer firstYear,
            Integer lastYear,
            long totalArticles,
            long distinctAuthors,
            long totalAuthors
    ) {
        public int yearSpan() {
            if (firstYear == null || lastYear == null) return 0;
            return lastYear - firstYear + 1;
        }

        public double avgAuthorsPerArticle() {
            return totalArticles == 0 ? 0.0 : (double) totalAuthors / totalArticles;
        }

        public double avgAuthorsPerYear() {
            int s = yearSpan();
            return s == 0 ? 0.0 : (double) totalAuthors / s;
        }

        public double avgArticlesPerYear() {
            int s = yearSpan();
            return s == 0 ? 0.0 : (double) totalArticles / s;
        }
    }
}
