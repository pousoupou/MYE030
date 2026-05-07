package gr.uoi.cs.mye030.repository;

import gr.uoi.cs.mye030.db.DatabaseConnectionManager;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.service.ChartData.ProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearlyAuthorCounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
        String sql = "SELECT v.id, v.journal_rank, v.title, v.acronym, v.country, v.best_subject_area, v.total_docs, v.total_refs "
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
                            rs.getInt("total_refs")));
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
}
