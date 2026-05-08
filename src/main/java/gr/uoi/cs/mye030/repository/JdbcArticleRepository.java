package gr.uoi.cs.mye030.repository;

import gr.uoi.cs.mye030.db.DatabaseConnectionManager;
import gr.uoi.cs.mye030.model.Article;
import gr.uoi.cs.mye030.model.ArticleType;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.service.ChartData.PublisherJournalQuarters;
import gr.uoi.cs.mye030.service.ChartData.YearProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearPublication;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class JdbcArticleRepository implements ArticleRepository {

    private final Supplier<Connection> connections;

    public JdbcArticleRepository() {
        this(() -> {
            try {
                return DatabaseConnectionManager.getConnection();
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to obtain DB connection", e);
            }
        });
    }

    public JdbcArticleRepository(Supplier<Connection> connections) {
        this.connections = connections;
    }

    @Override
    public List<Article> findByCriteria(FilterCriteria f) {
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String sql = "SELECT v.id, v.article_type, v.title, v.acronym, v.journal_id, v.conference_id, v.date_pub, v.publisher "
                + "FROM v_articles_full v"
                + sf.whereClause()
                + " ORDER BY v.date_pub";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            sf.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Article> out = new ArrayList<>();
                while (rs.next()) {
                    Date d = rs.getDate("date_pub");
                    out.add(new Article(
                            rs.getInt("id"),
                            ArticleType.fromType(rs.getString("article_type").charAt(0)),
                            rs.getString("title"),
                            rs.getString("acronym"),
                            (Integer) rs.getObject("journal_id"),
                            (Integer) rs.getObject("conference_id"),
                            d == null ? null : d.toLocalDate(),
                            rs.getString("publisher")
                    ));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("findByCriteria failed", e);
        }
    }

    @Override
    public Map<Integer, Long> countByYear(FilterCriteria f) {
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String sql = "SELECT v.year_pub AS y, COUNT(*) AS n "
                + "FROM v_articles_per_year v"
                + sf.whereClause()
                + " GROUP BY v.year_pub ORDER BY y";
        return runIntCount(sql, sf);
    }

    @Override
    public Map<String, Long> countByPublisher(FilterCriteria f) {
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String sql = "SELECT v.publisher AS k, COUNT(*) AS n "
                + "FROM v_articles_by_publisher v"
                + sf.whereClause()
                + " GROUP BY v.publisher ORDER BY n DESC LIMIT 25";
        return runStringCount(sql, sf);
    }

    public Map<Integer, Long> countByJournalRankBucket(FilterCriteria f) {
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String sql = "SELECT v.journal_rank AS k, COUNT(*) AS n "
                + "FROM v_articles_by_journal_rank v"
                + sf.whereClause()
                + " GROUP BY v.journal_rank ORDER BY v.journal_rank";
        return runIntCount(sql, sf);
    }

    @Override
    public YearProfileStats yearProfileStats(int year) {
        long totalArticles = 0L, distinctJournals = 0L, distinctConferences = 0L;
        long distinctAuthors = 0L, totalAuthors = 0L;

        String articleSql = "SELECT COUNT(*), COUNT(DISTINCT journal_id), COUNT(DISTINCT conference_id) "
                + "FROM articles WHERE YEAR(date_pub) = ?";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(articleSql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalArticles = rs.getLong(1);
                    distinctJournals = rs.getLong(2);
                    distinctConferences = rs.getLong(3);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("yearProfileStats articles failed", e);
        }

        String authorSql = "SELECT COUNT(*), COUNT(DISTINCT aa.author_id) "
                + "FROM articles a JOIN articles_authors aa ON aa.article_id = a.id "
                + "WHERE YEAR(a.date_pub) = ?";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(authorSql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalAuthors = rs.getLong(1);
                    distinctAuthors = rs.getLong(2);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("yearProfileStats authors failed", e);
        }

        return new YearProfileStats(totalArticles, distinctJournals, distinctConferences, distinctAuthors, totalAuthors);
    }

    @Override
    public List<YearPublication> publicationsForYear(int year, ArticleType type) {
        String sql = "SELECT a.id, a.title, "
                + "GROUP_CONCAT(au.author_name ORDER BY au.author_name SEPARATOR ', ') AS authors "
                + "FROM articles a "
                + "LEFT JOIN articles_authors aa ON aa.article_id = a.id "
                + "LEFT JOIN authors au ON au.id = aa.author_id "
                + "WHERE YEAR(a.date_pub) = ? AND a.article_type = ? "
                + "GROUP BY a.id, a.title ORDER BY a.title";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setString(2, String.valueOf(type.code()));
            try (ResultSet rs = ps.executeQuery()) {
                List<YearPublication> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new YearPublication(rs.getInt(1), rs.getString(2), rs.getString(3)));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("publicationsForYear failed", e);
        }
    }

    @Override
    public List<String> distinctJournalPublishers() {
        String sql = "SELECT DISTINCT publisher FROM articles "
                + "WHERE publisher IS NOT NULL AND publisher <> '' "
                + "AND article_type = 'J' AND journal_id IS NOT NULL "
                + "ORDER BY publisher";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> out = new ArrayList<>();
            while (rs.next()) out.add(rs.getString(1));
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("distinctJournalPublishers failed", e);
        }
    }

    @Override
    public List<PublisherJournalQuarters> journalQuartersByPublisher(Collection<String> publishers, FilterCriteria f) {
        if (publishers == null || publishers.isEmpty()) return List.of();
        List<String> yearClauses = new ArrayList<>();
        List<Object> yearParams = new ArrayList<>();
        if (f != null) {
            if (f.yearFrom() != null) { yearClauses.add("YEAR(a.date_pub) >= ?"); yearParams.add(f.yearFrom()); }
            if (f.yearTo() != null)   { yearClauses.add("YEAR(a.date_pub) <= ?"); yearParams.add(f.yearTo()); }
        }
        StringBuilder placeholders = new StringBuilder(publishers.size() * 2);
        for (int i = 0; i < publishers.size(); i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
        }
        StringBuilder where = new StringBuilder("WHERE a.article_type = 'J' AND a.journal_id IS NOT NULL "
                + "AND a.publisher IN (").append(placeholders).append(")");
        for (String yc : yearClauses) where.append(" AND ").append(yc);

        String sql = "SELECT a.publisher AS p, "
                + "COUNT(DISTINCT a.journal_id) AS total, "
                + "COUNT(DISTINCT CASE WHEN MONTH(a.date_pub) BETWEEN 1 AND 3 THEN a.journal_id END) AS q1, "
                + "COUNT(DISTINCT CASE WHEN MONTH(a.date_pub) BETWEEN 4 AND 6 THEN a.journal_id END) AS q2, "
                + "COUNT(DISTINCT CASE WHEN MONTH(a.date_pub) BETWEEN 7 AND 9 THEN a.journal_id END) AS q3, "
                + "COUNT(DISTINCT CASE WHEN MONTH(a.date_pub) BETWEEN 10 AND 12 THEN a.journal_id END) AS q4 "
                + "FROM articles a "
                + where
                + " GROUP BY a.publisher ORDER BY total DESC, a.publisher";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            for (String p : publishers) ps.setString(idx++, p);
            for (Object yp : yearParams) ps.setObject(idx++, yp);
            try (ResultSet rs = ps.executeQuery()) {
                List<PublisherJournalQuarters> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new PublisherJournalQuarters(
                            rs.getString("p"),
                            rs.getLong("total"),
                            rs.getLong("q1"),
                            rs.getLong("q2"),
                            rs.getLong("q3"),
                            rs.getLong("q4")
                    ));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("journalQuartersByPublisher failed", e);
        }
    }

    private Map<Integer, Long> runIntCount(String sql, SqlFilter sf) {
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            sf.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, Long> out = new LinkedHashMap<>();
                while (rs.next()) out.put(rs.getInt(1), rs.getLong(2));
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("query failed: " + sql, e);
        }
    }

    private Map<String, Long> runStringCount(String sql, SqlFilter sf) {
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            sf.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Long> out = new LinkedHashMap<>();
                while (rs.next()) out.put(rs.getString(1), rs.getLong(2));
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("query failed: " + sql, e);
        }
    }
}
