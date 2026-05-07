package gr.uoi.cs.mye030.repository;

import gr.uoi.cs.mye030.db.DatabaseConnectionManager;
import gr.uoi.cs.mye030.model.Author;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.service.ChartData.AuthorListRow;
import gr.uoi.cs.mye030.service.ChartData.AuthorProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearCount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class JdbcAuthorRepository implements AuthorRepository {

    private final Supplier<Connection> connections;

    public JdbcAuthorRepository() {
        this(() -> {
            try {
                return DatabaseConnectionManager.getConnection();
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to obtain DB connection", e);
            }
        });
    }

    public JdbcAuthorRepository(Supplier<Connection> connections) {
        this.connections = connections;
    }

    @Override
    public List<Map.Entry<Author, Long>> topByArticleCount(FilterCriteria f, int limit) {
        SqlFilter sf = SqlFilter.forArticleView(f, "v", true);
        String sql = "SELECT v.author_id AS id, v.author_name, COUNT(*) AS n "
                + "FROM v_top_authors v"
                + sf.whereClause()
                + " GROUP BY v.author_id, v.author_name"
                + " ORDER BY n DESC LIMIT ?";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = sf.bindStartingAt(ps, 1);
            ps.setInt(idx, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<Map.Entry<Author, Long>> out = new ArrayList<>();
                while (rs.next()) {
                    Author a = new Author(rs.getInt("id"), rs.getString("author_name"));
                    out.add(new AbstractMap.SimpleEntry<>(a, rs.getLong("n")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("topByArticleCount failed", e);
        }
    }

    @Override
    public List<AuthorListRow> findAll() {
        String sql = "SELECT v.author_id AS id, v.author_name, COUNT(*) AS n "
                + "FROM v_top_authors v"
                + " GROUP BY v.author_id, v.author_name"
                + " ORDER BY n DESC, v.author_name ASC";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<AuthorListRow> out = new ArrayList<>();
            while (rs.next()) {
                Author a = new Author(rs.getInt("id"), rs.getString("author_name"));
                out.add(new AuthorListRow(a, rs.getLong("n")));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("findAll authors failed", e);
        }
    }

    @Override
    public List<YearCount> articlesPerYear(int authorId) {
        String sql = "SELECT v.year_pub AS y, COUNT(*) AS n "
                + "FROM v_top_authors v"
                + " WHERE v.author_id = ?"
                + " GROUP BY v.year_pub"
                + " ORDER BY v.year_pub";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<YearCount> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new YearCount(rs.getInt("y"), rs.getLong("n")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("articlesPerYear (author) failed", e);
        }
    }

    @Override
    public AuthorProfileStats profileStats(int authorId) {
        String sql = "SELECT MIN(v.year_pub) AS min_y, MAX(v.year_pub) AS max_y, COUNT(*) AS n "
                + "FROM v_top_authors v"
                + " WHERE v.author_id = ?";
        try (Connection c = connections.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new AuthorProfileStats(null, null, 0);
                int minY = rs.getInt("min_y");
                Integer firstYear = rs.wasNull() ? null : minY;
                int maxY = rs.getInt("max_y");
                Integer lastYear = rs.wasNull() ? null : maxY;
                long n = rs.getLong("n");
                return new AuthorProfileStats(firstYear, lastYear, n);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("profileStats (author) failed", e);
        }
    }
}
