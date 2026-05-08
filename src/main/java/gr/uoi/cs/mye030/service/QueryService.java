package gr.uoi.cs.mye030.service;

import gr.uoi.cs.mye030.model.Author;
import gr.uoi.cs.mye030.model.Conference;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.repository.ArticleRepository;
import gr.uoi.cs.mye030.repository.AuthorRepository;
import gr.uoi.cs.mye030.repository.ConferenceRepository;
import gr.uoi.cs.mye030.repository.JournalRepository;
import gr.uoi.cs.mye030.model.ArticleType;
import gr.uoi.cs.mye030.service.ChartData.AuthorListRow;
import gr.uoi.cs.mye030.service.ChartData.AuthorProfileStats;
import gr.uoi.cs.mye030.service.ChartData.CategoryCount;
import gr.uoi.cs.mye030.service.ChartData.CountryCount;
import gr.uoi.cs.mye030.service.ChartData.EntityBarStats;
import gr.uoi.cs.mye030.service.ChartData.MultiYearSeries;
import gr.uoi.cs.mye030.service.ChartData.ProfileStats;
import gr.uoi.cs.mye030.service.ChartData.PublisherJournalQuarters;
import gr.uoi.cs.mye030.service.ChartData.YearCount;
import gr.uoi.cs.mye030.service.ChartData.YearProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearPublication;
import gr.uoi.cs.mye030.service.ChartData.YearlyAuthorCounts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class QueryService {

    private static final int DEFAULT_TOP_N = 10;

    private final ArticleRepository articles;
    private final AuthorRepository authors;
    private final JournalRepository journals;
    private final ConferenceRepository conferences;

    public QueryService(ArticleRepository articles,
                        AuthorRepository authors,
                        JournalRepository journals,
                        ConferenceRepository conferences) {
        this.articles = articles;
        this.authors = authors;
        this.journals = journals;
        this.conferences = conferences;
    }

    public List<YearCount> articlesPerYear(FilterCriteria f) {
        Map<Integer, Long> counts = articles.countByYear(f);
        List<YearCount> out = new ArrayList<>(counts.size());
        for (Map.Entry<Integer, Long> e : counts.entrySet()) {
            out.add(new YearCount(e.getKey(), e.getValue()));
        }
        return out;
    }

    public List<CategoryCount> topAuthorsByArticleCount(FilterCriteria f) {
        int limit = (f != null && f.topN() != null) ? f.topN() : DEFAULT_TOP_N;
        List<Map.Entry<Author, Long>> rows = authors.topByArticleCount(f, limit);
        List<CategoryCount> out = new ArrayList<>(rows.size());
        for (Map.Entry<Author, Long> e : rows) {
            out.add(new CategoryCount(e.getKey().name(), e.getValue()));
        }
        return out;
    }

    public List<CountryCount> journalsByCountry(FilterCriteria f) {
        Map<String, Long> counts = journals.countByCountry(f);
        List<CountryCount> out = new ArrayList<>(counts.size());
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            out.add(new CountryCount(e.getKey(), e.getValue()));
        }
        return out;
    }

    public List<CategoryCount> articlesByPublisher(FilterCriteria f) {
        Map<String, Long> counts = articles.countByPublisher(f);
        List<CategoryCount> out = new ArrayList<>(counts.size());
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            out.add(new CategoryCount(e.getKey(), e.getValue()));
        }
        return out;
    }

    public List<CategoryCount> conferencesByRank(FilterCriteria f) {
        Map<String, Long> counts = conferences.countByRank(f);
        List<CategoryCount> out = new ArrayList<>(counts.size());
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            out.add(new CategoryCount(e.getKey(), e.getValue()));
        }
        return out;
    }

    public List<CategoryCount> articlesByJournalRank(FilterCriteria f) {
        Map<Integer, Long> counts = articles.countByJournalRankBucket(f);
        List<CategoryCount> out = new ArrayList<>(counts.size());
        for (Map.Entry<Integer, Long> e : counts.entrySet()) {
            out.add(new CategoryCount("Rank " + e.getKey(), e.getValue()));
        }
        return out;
    }

    public List<Journal> findJournals(FilterCriteria f) {
        return journals.findByCriteria(f);
    }

    public List<Conference> findConferences(FilterCriteria f) {
        return conferences.findByCriteria(f);
    }

    public List<YearCount> articlesPerYearForJournal(int journalId, FilterCriteria f) {
        return toYearCounts(journals.articlesPerYear(journalId, f));
    }

    public List<YearlyAuthorCounts> authorsPerYearForJournal(int journalId, FilterCriteria f) {
        return journals.authorsPerYear(journalId, f);
    }

    public List<YearCount> articlesPerYearForConference(int conferenceId, FilterCriteria f) {
        return toYearCounts(conferences.articlesPerYear(conferenceId, f));
    }

    public List<YearlyAuthorCounts> authorsPerYearForConference(int conferenceId, FilterCriteria f) {
        return conferences.authorsPerYear(conferenceId, f);
    }

    public List<AuthorListRow> findAuthors() {
        return authors.findAll();
    }

    public List<YearCount> articlesPerYearForAuthor(int authorId) {
        return authors.articlesPerYear(authorId);
    }

    public AuthorProfileStats authorProfileStats(int authorId) {
        return authors.profileStats(authorId);
    }

    public ProfileStats profileStatsForJournal(int journalId, FilterCriteria f) {
        return journals.profileStats(journalId, f);
    }

    public ProfileStats profileStatsForConference(int conferenceId, FilterCriteria f) {
        return conferences.profileStats(conferenceId, f);
    }

    public YearProfileStats yearProfileStats(int year) {
        return articles.yearProfileStats(year);
    }

    public List<YearPublication> journalPublicationsForYear(int year) {
        return articles.publicationsForYear(year, ArticleType.JOURNAL);
    }

    public List<YearPublication> conferencePublicationsForYear(int year) {
        return articles.publicationsForYear(year, ArticleType.CONFERENCE);
    }

    public List<MultiYearSeries> articlesPerYearForJournals(Collection<Integer> journalIds, FilterCriteria f) {
        return journals.articlesPerYearForJournals(journalIds, f);
    }

    public List<MultiYearSeries> articlesPerYearForConferences(Collection<Integer> conferenceIds, FilterCriteria f) {
        return conferences.articlesPerYearForConferences(conferenceIds, f);
    }

    public List<MultiYearSeries> activeJournalsBySubjectAreaPerYear(Collection<String> subjectAreas, FilterCriteria f) {
        return journals.activeJournalsBySubjectAreaPerYear(subjectAreas, f);
    }

    public List<MultiYearSeries> activeConferencesByPrimaryForPerYear(Collection<String> primaryFoRs, FilterCriteria f) {
        return conferences.activeConferencesByPrimaryForPerYear(primaryFoRs, f);
    }

    public List<String> distinctSubjectAreas() {
        return journals.distinctSubjectAreas();
    }

    public List<String> distinctPrimaryFoRs() {
        return conferences.distinctPrimaryFoRs();
    }

    public List<EntityBarStats> barStatsForJournals(Collection<Integer> journalIds, FilterCriteria f) {
        return journals.barStatsForJournals(journalIds, f);
    }

    public List<EntityBarStats> barStatsForConferences(Collection<Integer> conferenceIds, FilterCriteria f) {
        return conferences.barStatsForConferences(conferenceIds, f);
    }

    public List<String> distinctJournalPublishers() {
        return articles.distinctJournalPublishers();
    }

    public List<PublisherJournalQuarters> journalQuartersByPublisher(Collection<String> publishers, FilterCriteria f) {
        return articles.journalQuartersByPublisher(publishers, f);
    }

    private static List<YearCount> toYearCounts(Map<Integer, Long> counts) {
        List<YearCount> out = new ArrayList<>(counts.size());
        for (Map.Entry<Integer, Long> e : counts.entrySet()) {
            out.add(new YearCount(e.getKey(), e.getValue()));
        }
        return out;
    }
}
