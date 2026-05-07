package gr.uoi.cs.mye030.repository;

import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.service.ChartData.ProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearlyAuthorCounts;

import java.util.List;
import java.util.Map;

public interface JournalRepository {
    List<Journal> findByCriteria(FilterCriteria f);

    Map<String, Long> countByCountry(FilterCriteria f);

    Map<Integer, Long> countByRank(FilterCriteria f);

    Map<Integer, Long> articlesPerYear(int journalId, FilterCriteria f);

    List<YearlyAuthorCounts> authorsPerYear(int journalId, FilterCriteria f);

    ProfileStats profileStats(int journalId, FilterCriteria f);
}
