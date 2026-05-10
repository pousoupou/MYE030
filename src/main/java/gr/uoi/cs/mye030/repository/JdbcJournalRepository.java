package gr.uoi.cs.mye030.repository;

import gr.uoi.cs.mye030.db.DatabaseConnectionManager;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.service.ChartData.EntityBarStats;
import gr.uoi.cs.mye030.service.ChartData.MultiYearSeries;
import gr.uoi.cs.mye030.service.ChartData.ProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearCount;
import gr.uoi.cs.mye030.service.ChartData.YearlyAuthorCounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class JdbcJournalRepository implements JournalRepository {

    private final Supplier<Connection> connections;

    public JdbcJournalRepository() {
        this(() -> {
            try {
                return DatabaseConnectionManager.getConnection();
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to obtain DB connection", e);
            }
        });
    }

    public JdbcJournalRepository(Supplier<Connection> connections) {
        this.connections = connections;
    }

    @Override
    public List<Journal> findByCriteria(FilterCriteria f) {
        SqlFilter sf = SqlFilter.forJournalView(f, "v");
        String sql = "SELECT v.id, v.journal_rank, v.title, v.acronym, v.country, v.best_subject_area, v.total_docs, v.total_refs, v.publisher "
                + "FROM v_journals_full v" + sf.whereClause() + " ORDER BY v.journal_rank";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            sf.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Journal> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Journal(
                            rs.getInt("id"),
                            rs.getInt("journal_rank"),
                            rs.getString("title"),
                            rs.getString("acronym"),
                            rs.getString("country"),
                            rs.getString("best_subject_area"),
                            rs.getInt("total_docs"),
                            rs.getInt("total_refs"),
                            rs.getString("publisher")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("findByCriteria failed", e);
        }
    }

    @Override
    public Map<String, Long> countByCountry(FilterCriteria f) {
        SqlFilter sf = SqlFilter.forJournalView(f, "v");
        String sql = "SELECT v.country AS k, COUNT(*) AS n FROM v_journals_by_country v"
                + sf.whereClause()
                + " GROUP BY v.country ORDER BY n DESC LIMIT 25";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            sf.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Long> out = new LinkedHashMap<>();
                while (rs.next()) out.put(rs.getString(1), rs.getLong(2));
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("countByCountry failed", e);
        }
    }

    @Override
    public Map<Integer, Long> countByRank(FilterCriteria f) {
        SqlFilter sf = SqlFilter.forJournalView(f, "v");
        String sql = "SELECT v.journal_rank AS k, COUNT(*) AS n FROM v_journals_full v"
                + sf.whereClause()
                + " GROUP BY v.journal_rank ORDER BY v.journal_rank";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            sf.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, Long> out = new LinkedHashMap<>();
                while (rs.next()) out.put(rs.getInt(1), rs.getLong(2));
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("countByRank failed", e);
        }
    }

    @Override
    public Map<Integer, Long> articlesPerYear(int journalId, FilterCriteria f) {
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String where = sf.whereClause();
        String composed = where.isEmpty()
                ? " WHERE v.journal_id = ?"
                : where + " AND v.journal_id = ?";
        String sql = "SELECT v.year_pub AS y, COUNT(*) AS n FROM v_journal_articles_per_year v"
                + composed
                + " GROUP BY v.year_pub ORDER BY y";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = sf.bindStartingAt(ps, 1);
            ps.setInt(idx, journalId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, Long> out = new LinkedHashMap<>();
                while (rs.next()) out.put(rs.getInt(1), rs.getLong(2));
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("articlesPerYear (journal) failed", e);
        }
    }

    @Override
    public List<YearlyAuthorCounts> authorsPerYear(int journalId, FilterCriteria f) {
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String where = sf.whereClause();
        String composed = where.isEmpty()
                ? " WHERE v.journal_id = ?"
                : where + " AND v.journal_id = ?";
        String sql = "SELECT v.year_pub AS y, COUNT(DISTINCT v.author_id) AS d, COUNT(*) AS t "
                + "FROM v_journal_authors_per_year v"
                + composed
                + " GROUP BY v.year_pub ORDER BY y";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = sf.bindStartingAt(ps, 1);
            ps.setInt(idx, journalId);
            try (ResultSet rs = ps.executeQuery()) {
                List<YearlyAuthorCounts> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new YearlyAuthorCounts(rs.getInt(1), rs.getLong(2), rs.getLong(3)));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("authorsPerYear (journal) failed", e);
        }
    }

    @Override
    public ProfileStats profileStats(int journalId, FilterCriteria f) {
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String where = sf.whereClause();
        String composed = where.isEmpty()
                ? " WHERE v.journal_id = ?"
                : where + " AND v.journal_id = ?";

        Integer firstYear = null;
        Integer lastYear = null;
        long totalArticles = 0L;
        long distinctAuthors = 0L;
        long totalAuthors = 0L;

        String articlesSql = "SELECT MIN(v.year_pub), MAX(v.year_pub), COUNT(DISTINCT v.article_id) "
                + "FROM v_journal_articles_per_year v" + composed;
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(articlesSql)) {
            int idx = sf.bindStartingAt(ps, 1);
            ps.setInt(idx, journalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int y1 = rs.getInt(1);
                    if (!rs.wasNull()) firstYear = y1;
                    int y2 = rs.getInt(2);
                    if (!rs.wasNull()) lastYear = y2;
                    totalArticles = rs.getLong(3);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("profileStats articles (journal) failed", e);
        }

        String authorsSql = "SELECT COUNT(DISTINCT v.author_id), COUNT(*) "
                + "FROM v_journal_authors_per_year v" + composed;
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(authorsSql)) {
            int idx = sf.bindStartingAt(ps, 1);
            ps.setInt(idx, journalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    distinctAuthors = rs.getLong(1);
                    totalAuthors = rs.getLong(2);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("profileStats authors (journal) failed", e);
        }

        return new ProfileStats(firstYear, lastYear, totalArticles, distinctAuthors, totalAuthors);
    }

    @Override
    public List<MultiYearSeries> articlesPerYearForJournals(Collection<Integer> journalIds, FilterCriteria f) {
        if (journalIds == null || journalIds.isEmpty()) return List.of();
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String where = sf.whereClause();
        String idsList = repeatPlaceholders(journalIds.size());
        String composed = (where.isEmpty() ? " WHERE " : where + " AND ")
                + "v.journal_id IN (" + idsList + ")";
        String sql = "SELECT v.journal_id AS jid, j.title AS jtitle, v.year_pub AS y, COUNT(*) AS n "
                + "FROM v_journal_articles_per_year v"
                + " JOIN journals j ON j.id = v.journal_id"
                + composed
                + " GROUP BY v.journal_id, j.title, v.year_pub"
                + " ORDER BY j.title, y";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = sf.bindStartingAt(ps, 1);
            for (Integer id : journalIds) ps.setInt(idx++, id);
            try (ResultSet rs = ps.executeQuery()) {
                return collectSeriesById(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("articlesPerYearForJournals failed", e);
        }
    }

    @Override
    public List<MultiYearSeries> activeJournalsBySubjectAreaPerYear(Collection<String> subjectAreas, FilterCriteria f) {
        if (subjectAreas == null || subjectAreas.isEmpty()) return List.of();
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String where = sf.whereClause();
        String catList = repeatPlaceholders(subjectAreas.size());
        String composed = (where.isEmpty() ? " WHERE " : where + " AND ")
                + "j.best_subject_area IN (" + catList + ")";
        String sql = "SELECT j.best_subject_area AS cat, v.year_pub AS y, COUNT(DISTINCT v.journal_id) AS n "
                + "FROM v_journal_articles_per_year v"
                + " JOIN journals j ON j.id = v.journal_id"
                + composed
                + " GROUP BY j.best_subject_area, v.year_pub"
                + " ORDER BY j.best_subject_area, y";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = sf.bindStartingAt(ps, 1);
            for (String s : subjectAreas) ps.setString(idx++, s);
            try (ResultSet rs = ps.executeQuery()) {
                return collectSeriesByName(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("activeJournalsBySubjectAreaPerYear failed", e);
        }
    }

    @Override
    public List<String> distinctSubjectAreas() {
        String sql = "SELECT DISTINCT best_subject_area FROM journals "
                + "WHERE best_subject_area IS NOT NULL AND best_subject_area <> '' "
                + "ORDER BY best_subject_area";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> out = new ArrayList<>();
            while (rs.next()) out.add(rs.getString(1));
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("distinctSubjectAreas failed", e);
        }
    }

    private static List<MultiYearSeries> collectSeriesById(ResultSet rs) throws SQLException {
        Map<Integer, String> names = new LinkedHashMap<>();
        Map<Integer, List<YearCount>> points = new LinkedHashMap<>();
        while (rs.next()) {
            int id = rs.getInt(1);
            String name = rs.getString(2);
            int y = rs.getInt(3);
            long n = rs.getLong(4);
            names.putIfAbsent(id, name);
            points.computeIfAbsent(id, k -> new ArrayList<>()).add(new YearCount(y, n));
        }
        List<MultiYearSeries> out = new ArrayList<>(names.size());
        for (Map.Entry<Integer, String> e : names.entrySet()) {
            out.add(new MultiYearSeries(e.getValue(), Collections.unmodifiableList(points.get(e.getKey()))));
        }
        return out;
    }

    private static List<MultiYearSeries> collectSeriesByName(ResultSet rs) throws SQLException {
        Map<String, List<YearCount>> points = new LinkedHashMap<>();
        while (rs.next()) {
            String name = rs.getString(1);
            int y = rs.getInt(2);
            long n = rs.getLong(3);
            points.computeIfAbsent(name, k -> new ArrayList<>()).add(new YearCount(y, n));
        }
        List<MultiYearSeries> out = new ArrayList<>(points.size());
        for (Map.Entry<String, List<YearCount>> e : points.entrySet()) {
            out.add(new MultiYearSeries(e.getKey(), Collections.unmodifiableList(e.getValue())));
        }
        return out;
    }

    @Override
    public List<EntityBarStats> barStatsForJournals(Collection<Integer> journalIds, FilterCriteria f) {
        if (journalIds == null || journalIds.isEmpty()) return List.of();
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String where = sf.whereClause();
        String idsList = repeatPlaceholders(journalIds.size());
        String composed = (where.isEmpty() ? " WHERE " : where + " AND ")
                + "v.journal_id IN (" + idsList + ")";

        String articlesSql = "SELECT v.journal_id AS id, j.title AS name, "
                + "COUNT(DISTINCT v.article_id) AS total_articles, "
                + "MIN(v.year_pub) AS first_y, MAX(v.year_pub) AS last_y "
                + "FROM v_journal_articles_per_year v "
                + "JOIN journals j ON j.id = v.journal_id"
                + composed
                + " GROUP BY v.journal_id, j.title";

        Map<Integer, String> names = new LinkedHashMap<>();
        Map<Integer, long[]> art = new LinkedHashMap<>();
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(articlesSql)) {
            int idx = sf.bindStartingAt(ps, 1);
            for (Integer id : journalIds) ps.setInt(idx++, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    names.put(id, rs.getString("name"));
                    long total = rs.getLong("total_articles");
                    int y1 = rs.getInt("first_y"); boolean y1null = rs.wasNull();
                    int y2 = rs.getInt("last_y");  boolean y2null = rs.wasNull();
                    long span = (y1null || y2null) ? 0L : (y2 - y1 + 1L);
                    art.put(id, new long[] { total, span });
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("barStatsForJournals (articles) failed", e);
        }

        String authorsSql = "SELECT v.journal_id AS id, COUNT(*) AS total_authors "
                + "FROM v_journal_authors_per_year v"
                + composed
                + " GROUP BY v.journal_id";
        Map<Integer, Long> totalAuthors = new LinkedHashMap<>();
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(authorsSql)) {
            int idx = sf.bindStartingAt(ps, 1);
            for (Integer id : journalIds) ps.setInt(idx++, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) totalAuthors.put(rs.getInt("id"), rs.getLong("total_authors"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("barStatsForJournals (authors) failed", e);
        }

        List<EntityBarStats> out = new ArrayList<>(names.size());
        for (Map.Entry<Integer, String> e : names.entrySet()) {
            int id = e.getKey();
            long[] a = art.get(id);
            long total = a[0];
            long span = a[1];
            long auth = totalAuthors.getOrDefault(id, 0L);
            double avgArt = span == 0 ? 0.0 : (double) total / span;
            double avgAuth = span == 0 ? 0.0 : (double) auth / span;
            out.add(new EntityBarStats(id, e.getValue(), total, avgArt, avgAuth));
        }
        return out;
    }

    private static String repeatPlaceholders(int n) {
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }
}
