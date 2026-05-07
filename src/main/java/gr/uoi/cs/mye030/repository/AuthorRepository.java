package gr.uoi.cs.mye030.repository;

import gr.uoi.cs.mye030.model.Author;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.service.ChartData.AuthorListRow;
import gr.uoi.cs.mye030.service.ChartData.AuthorProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearCount;

import java.util.List;
import java.util.Map;

public interface AuthorRepository {
    List<Map.Entry<Author, Long>> topByArticleCount(FilterCriteria f, int limit);

    List<AuthorListRow> findAll();

    List<YearCount> articlesPerYear(int authorId);

    AuthorProfileStats profileStats(int authorId);
}
