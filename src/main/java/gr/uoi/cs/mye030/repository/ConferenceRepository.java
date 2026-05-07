package gr.uoi.cs.mye030.repository;

import gr.uoi.cs.mye030.model.Conference;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.service.ChartData.ProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearlyAuthorCounts;

import java.util.List;
import java.util.Map;

public interface ConferenceRepository {
    List<Conference> findByCriteria(FilterCriteria f);

    Map<String, Long> countByRank(FilterCriteria f);

    Map<Integer, Long> articlesPerYear(int conferenceId, FilterCriteria f);

    List<YearlyAuthorCounts> authorsPerYear(int conferenceId, FilterCriteria f);

    ProfileStats profileStats(int conferenceId, FilterCriteria f);
}
