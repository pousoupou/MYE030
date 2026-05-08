package gr.uoi.cs.mye030.repository;

import gr.uoi.cs.mye030.model.Article;
import gr.uoi.cs.mye030.model.ArticleType;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.service.ChartData.PublisherJournalQuarters;
import gr.uoi.cs.mye030.service.ChartData.YearProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearPublication;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ArticleRepository {
    List<Article> findByCriteria(FilterCriteria f);

    Map<Integer, Long> countByYear(FilterCriteria f);

    Map<String, Long> countByPublisher(FilterCriteria f);

    Map<Integer, Long> countByJournalRankBucket(FilterCriteria f);

    YearProfileStats yearProfileStats(int year);

    List<YearPublication> publicationsForYear(int year, ArticleType type);

    List<String> distinctJournalPublishers();

    List<PublisherJournalQuarters> journalQuartersByPublisher(Collection<String> publishers, FilterCriteria f);
}
