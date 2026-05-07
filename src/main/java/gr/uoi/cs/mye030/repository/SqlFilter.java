package gr.uoi.cs.mye030.repository;

import gr.uoi.cs.mye030.model.FilterCriteria;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class SqlFilter {

    private final List<String> clauses = new ArrayList<>();
    private final List<Object> params = new ArrayList<>();

    private SqlFilter() {}

    static SqlFilter forArticleView(FilterCriteria f, String alias, boolean hasCountry) {
        SqlFilter sf = new SqlFilter();
        if (f == null) return sf;
        if (f.type() != null) {
            sf.clauses.add(alias + ".article_type = ?");
            sf.params.add(String.valueOf(f.type().code()));
        }
        if (f.yearFrom() != null) {
            sf.clauses.add(alias + ".year_pub >= ?");
            sf.params.add(f.yearFrom());
        }
        if (f.yearTo() != null) {
            sf.clauses.add(alias + ".year_pub <= ?");
            sf.params.add(f.yearTo());
        }
        if (f.publisher() != null && !f.publisher().isBlank()) {
            sf.clauses.add(alias + ".publisher LIKE ?");
            sf.params.add("%" + f.publisher() + "%");
        }
        if (hasCountry && f.countries() != null && !f.countries().isEmpty()) {
            sf.clauses.add(alias + ".country IN (" + placeholders(f.countries().size()) + ")");
            sf.params.addAll(f.countries());
        }
        return sf;
    }

    static SqlFilter forJournalView(FilterCriteria f, String alias) {
        SqlFilter sf = new SqlFilter();
        if (f == null) return sf;
        if (f.countries() != null && !f.countries().isEmpty()) {
            sf.clauses.add(alias + ".country IN (" + placeholders(f.countries().size()) + ")");
            sf.params.addAll(f.countries());
        }
        if (f.ranks() != null && !f.ranks().isEmpty()) {
            List<Integer> numericRanks = new ArrayList<>();
            for (String r : f.ranks()) {
                try { numericRanks.add(Integer.parseInt(r.trim())); } catch (NumberFormatException ignored) {}
            }
            if (!numericRanks.isEmpty()) {
                sf.clauses.add(alias + ".journal_rank IN (" + placeholders(numericRanks.size()) + ")");
                sf.params.addAll(numericRanks);
            }
        }
        return sf;
    }

    static SqlFilter forConferenceView(FilterCriteria f, String alias) {
        SqlFilter sf = new SqlFilter();
        if (f == null) return sf;
        if (f.ranks() != null && !f.ranks().isEmpty()) {
            sf.clauses.add(alias + ".conf_rank IN (" + placeholders(f.ranks().size()) + ")");
            sf.params.addAll(f.ranks());
        }
        return sf;
    }

    String whereClause() {
        if (clauses.isEmpty()) return "";
        return " WHERE " + String.join(" AND ", clauses);
    }

    void bind(PreparedStatement ps) throws SQLException {
        bindStartingAt(ps, 1);
    }

    int bindStartingAt(PreparedStatement ps, int startIndex) throws SQLException {
        int idx = startIndex;
        for (Object p : params) {
            ps.setObject(idx++, p);
        }
        return idx;
    }

    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }
}
