package gr.uoi.cs.mye030.etl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataExtractAndTransform {

    private static final String DATA_DIR = "../resources/data/";
    private static final String OUT_DIR = "../resources/data/out/";

    private static final String INPUT_ARTICLE = DATA_DIR + "input_article.csv";
    private static final String INPUT_INPROCEEDINGS = DATA_DIR + "input_inproceedings.csv";
    private static final String INPUT_JOURNALS = DATA_DIR + "journal_ranking_data_raw.tsv";
    private static final String INPUT_CONFERENCES = DATA_DIR + "iCore26_KilledColumnsForLoading.csv";

    private static final String OUT_AUTHORS = OUT_DIR + "authors.csv";
    private static final String OUT_ARTICLES = OUT_DIR + "articles.csv";
    private static final String OUT_ARTICLES_AUTHORS = OUT_DIR + "articles_authors.csv";
    private static final String OUT_JOURNALS = OUT_DIR + "journals.csv";
    private static final String OUT_CONFERENCES = OUT_DIR + "conferences.csv";

    private static final char DELIM = ';';
    private static final char TSV_DELIM = '\t';

    public static void main(String[] args) throws IOException {
        new java.io.File(OUT_DIR).mkdirs();

        Map<String, Integer> authorIds = loadAuthors();
        int[] nextJournalId = { 1 };
        Map<String, Integer> journalIdsByAcronym = loadJournals(nextJournalId);
        appendUnrankedJournals(journalIdsByAcronym, nextJournalId);
        Map<String, Integer> conferenceIdsByAcronym = loadConferences();
        loadArticles(authorIds, journalIdsByAcronym, conferenceIdsByAcronym);

        System.out.println("ETL finished. Output in " + OUT_DIR);
    }

    private static Map<String, Integer> loadAuthors() throws IOException {
        Set<String> authors = new LinkedHashSet<>();

        collectAuthors(INPUT_ARTICLE, authors);
        collectAuthors(INPUT_INPROCEEDINGS, authors);

        Map<String, Integer> ids = new HashMap<>();
        try (BufferedWriter w = new BufferedWriter(new FileWriter(OUT_AUTHORS))) {
            w.write("id,author_name");
            w.newLine();
            int id = 1;
            for (String a : authors) {
                ids.put(a, id);
                w.write(id + "," + csvEscape(a));
                w.newLine();
                id++;
            }
        }
        System.out.println("Wrote " + ids.size() + " authors");
        return ids;
    }

    private static void collectAuthors(String path, Set<String> authors) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line;
            int maxLength = -1;
            boolean headerSeen = false;
            StringBuilder logical = new StringBuilder();

            while ((line = r.readLine()) != null) {
                logical.append(line);
                String record = logical.toString();
                // simple multi-line record handling: count quotes; if unbalanced, keep reading
                if (countChar(record, '"') % 2 != 0) {
                    logical.append('\n');
                    continue;
                }
                logical.setLength(0);

                List<String> row = parseCsvLine(record, DELIM);
                if (!headerSeen) {
                    if (!row.isEmpty() && "id".equals(row.get(0))) {
                        maxLength = row.size();
                        headerSeen = true;
                    }
                    continue;
                }
                if (row.size() != maxLength) continue;
                String authorsField = row.get(1);
                if (authorsField.isEmpty()) continue;
                for (String a : authorsField.split("\\|")) {
                    if (!a.isEmpty()) authors.add(a);
                }
            }
        }
    }

    private static void loadArticles(
            Map<String, Integer> authorIds,
            Map<String, Integer> journalIdsByAcronym,
            Map<String, Integer> conferenceIdsByAcronym) throws IOException {
        try (BufferedWriter articlesW = new BufferedWriter(new FileWriter(OUT_ARTICLES));
             BufferedWriter aaW = new BufferedWriter(new FileWriter(OUT_ARTICLES_AUTHORS))) {

            articlesW.write("id;article_type;title;acronym;date_pub;publisher;journal_id;conference_id");
            articlesW.newLine();
            aaW.write("article_id,author_id");
            aaW.newLine();

            int[] nextId = { 1 };

            processArticles(INPUT_ARTICLE, 'J', nextId, authorIds,
                    journalIdsByAcronym, conferenceIdsByAcronym, articlesW, aaW);
            processArticles(INPUT_INPROCEEDINGS, 'C', nextId, authorIds,
                    journalIdsByAcronym, conferenceIdsByAcronym, articlesW, aaW);

            System.out.println("Wrote " + (nextId[0] - 1) + " articles");
        }
    }

    private static void processArticles(
            String path,
            char type,
            int[] nextId,
            Map<String, Integer> authorIds,
            Map<String, Integer> journalIdsByAcronym,
            Map<String, Integer> conferenceIdsByAcronym,
            BufferedWriter articlesW,
            BufferedWriter aaW) throws IOException {

        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line;
            int maxLength = -1;
            boolean headerSeen = false;
            StringBuilder logical = new StringBuilder();

            while ((line = r.readLine()) != null) {
                logical.append(line);
                String record = logical.toString();
                if (countChar(record, '"') % 2 != 0) {
                    logical.append('\n');
                    continue;
                }
                logical.setLength(0);

                List<String> row = parseCsvLine(record, DELIM);
                if (!headerSeen) {
                    if (!row.isEmpty() && "id".equals(row.get(0))) {
                        maxLength = row.size();
                        headerSeen = true;
                    }
                    continue;
                }
                if (row.size() != maxLength) continue;

                String title;
                String acronym;
                String datePub;
                String publisher;
                String authorsField = row.get(1);

                if (type == 'J') {
                    if (authorsField.isEmpty() || row.get(10).isEmpty() || row.get(12).isEmpty()
                            || (row.get(23).isEmpty() && row.get(24).isEmpty())) {
                        continue;
                    }
                    title = !row.get(23).isEmpty() ? row.get(23) : row.get(24);
                    acronym = onlyUpper(row.get(10));
                    datePub = row.get(12);
                    publisher = row.get(17);
                } else {
                    // input_inproceedings.csv
                    if (authorsField.isEmpty() || row.get(2).isEmpty() || row.get(11).isEmpty()
                            || (row.get(19).isEmpty() && row.get(20).isEmpty())) {
                        continue;
                    }
                    title = !row.get(19).isEmpty() ? row.get(19) : row.get(20);
                    acronym = row.get(2);
                    datePub = row.get(11);
                    publisher = null;
                }

                int articleId = nextId[0]++;
                String journalIdField = "";
                String conferenceIdField = "";
                if (type == 'J') {
                    Integer jid = journalIdsByAcronym.get(acronym);
                    if (jid != null) journalIdField = jid.toString();
                } else {
                    Integer cid = conferenceIdsByAcronym.get(acronym);
                    if (cid != null) conferenceIdField = cid.toString();
                }
                articlesW.write(articleId + ";" + type + ";" + csvEscape(title) + ";"
                        + csvEscape(acronym) + ";" + csvEscape(datePub) + ";" + csvEscape(publisher)
                        + ";" + journalIdField + ";" + conferenceIdField);
                articlesW.newLine();

                Set<Integer> seenAuthorIds = new LinkedHashSet<>();
                for (String a : authorsField.split("\\|")) {
                    Integer authorId = authorIds.get(a);
                    if (authorId != null) seenAuthorIds.add(authorId);
                }
                for (Integer authorId : seenAuthorIds) {
                    aaW.write(articleId + "," + authorId);
                    aaW.newLine();
                }
            }
        }
    }

    private static Map<String, Integer> loadJournals(int[] nextIdHolder) throws IOException {
        Map<String, Integer> acronymToId = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new FileReader(INPUT_JOURNALS));
             BufferedWriter w = new BufferedWriter(new FileWriter(OUT_JOURNALS))) {

            w.write("id;journal_rank;title;acronym;country;best_subject_area;total_docs;total_refs");
            w.newLine();

            String line;
            boolean headerSeen = false;
            int maxLength = -1;
            int nextId = nextIdHolder[0];
            StringBuilder logical = new StringBuilder();

            while ((line = r.readLine()) != null) {
                logical.append(line);
                String record = logical.toString();
                if (countChar(record, '"') % 2 != 0) {
                    logical.append('\n');
                    continue;
                }
                logical.setLength(0);

                List<String> row = parseCsvLine(record, TSV_DELIM);
                if (!headerSeen) {
                    if (!row.isEmpty() && "Rank".equals(row.get(0))) {
                        maxLength = row.size();
                        headerSeen = true;
                    }
                    continue;
                }
                if (row.size() != maxLength) continue;

                String rankStr = row.get(0);
                String title = normalizeJournalTitle(row.get(1));
                String country = row.get(3);
                String bestSubjectArea = row.get(9);
                String totalDocsStr = row.get(11);
                String totalRefsStr = row.get(13);

                if (rankStr.isEmpty() || title.isEmpty() || country.isEmpty()
                        || bestSubjectArea.isEmpty() || totalDocsStr.isEmpty() || totalRefsStr.isEmpty()) {
                    continue;
                }

                int journalId = nextId++;
                String acronym = onlyUpper(title);
                acronymToId.putIfAbsent(acronym, journalId);
                w.write(journalId + ";" + rankStr + ";" + csvEscape(title) + ";"
                        + csvEscape(acronym) + ";" + csvEscape(country) + ";"
                        + csvEscape(bestSubjectArea) + ";" + totalDocsStr + ";" + totalRefsStr);
                w.newLine();
            }
            System.out.println("Wrote " + (nextId - 1) + " journals");
            nextIdHolder[0] = nextId;
        }
        return acronymToId;
    }

    private static void appendUnrankedJournals(
            Map<String, Integer> acronymToId, int[] nextIdHolder) throws IOException {
        // Harvest journal titles referenced by input_article.csv (column 10) but
        // missing from the SCImago ranking source. Tech-report labs like
        // "GTE Laboratories Incorporated" land here. Stub rows leave rank/
        // country/best_subject_area/total_docs/total_refs empty (-> NULL on load).
        Map<String, String> newAcronymToTitle = new LinkedHashMap<>();

        try (BufferedReader r = new BufferedReader(new FileReader(INPUT_ARTICLE))) {
            String line;
            int maxLength = -1;
            boolean headerSeen = false;
            StringBuilder logical = new StringBuilder();

            while ((line = r.readLine()) != null) {
                logical.append(line);
                String record = logical.toString();
                if (countChar(record, '"') % 2 != 0) {
                    logical.append('\n');
                    continue;
                }
                logical.setLength(0);

                List<String> row = parseCsvLine(record, DELIM);
                if (!headerSeen) {
                    if (!row.isEmpty() && "id".equals(row.get(0))) {
                        maxLength = row.size();
                        headerSeen = true;
                    }
                    continue;
                }
                if (row.size() != maxLength) continue;

                // Mirror processArticles's drop filter so we only register
                // journals an article will actually reference.
                String authorsField = row.get(1);
                if (authorsField.isEmpty() || row.get(10).isEmpty() || row.get(12).isEmpty()
                        || (row.get(23).isEmpty() && row.get(24).isEmpty())) {
                    continue;
                }

                String journalTitle = row.get(10);
                String acronym = onlyUpper(journalTitle);
                if (acronym.isEmpty()) continue;
                if (acronymToId.containsKey(acronym)) continue;
                newAcronymToTitle.putIfAbsent(acronym, journalTitle);
            }
        }

        if (newAcronymToTitle.isEmpty()) {
            System.out.println("No unranked journals to append");
            return;
        }

        int nextId = nextIdHolder[0];
        try (BufferedWriter w = new BufferedWriter(new FileWriter(OUT_JOURNALS, true))) {
            for (Map.Entry<String, String> e : newAcronymToTitle.entrySet()) {
                String acronym = e.getKey();
                String title = e.getValue();
                int id = nextId++;
                acronymToId.put(acronym, id);
                w.write(id + ";;" + csvEscape(title) + ";" + csvEscape(acronym) + ";;;;");
                w.newLine();
            }
        }
        System.out.println("Appended " + newAcronymToTitle.size() + " unranked journals");
        nextIdHolder[0] = nextId;
    }

    private static Map<String, Integer> loadConferences() throws IOException {
        Map<String, Integer> acronymToId = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new FileReader(INPUT_CONFERENCES));
             BufferedWriter w = new BufferedWriter(new FileWriter(OUT_CONFERENCES))) {

            w.write("id;conf_name;acronym;conf_rank;primaryFoR");
            w.newLine();

            String line;
            boolean headerSeen = false;
            int maxLength = -1;
            int nextId = 1;
            StringBuilder logical = new StringBuilder();

            while ((line = r.readLine()) != null) {
                logical.append(line);
                String record = logical.toString();
                if (countChar(record, '"') % 2 != 0) {
                    logical.append('\n');
                    continue;
                }
                logical.setLength(0);

                List<String> row = parseCsvLine(record, ',');
                if (!headerSeen) {
                    if (!row.isEmpty() && "ID".equals(row.get(0))) {
                        maxLength = row.size();
                        headerSeen = true;
                    }
                    continue;
                }
                if (row.size() != maxLength) continue;

                String confName = row.get(1).trim();
                String acronym = row.get(2).trim();
                String confRank = row.get(4).trim();
                String primaryFoR = row.get(6).trim();

                if (confName.isEmpty() || acronym.isEmpty() || confRank.isEmpty() || primaryFoR.isEmpty()) {
                    continue;
                }

                int conferenceId = nextId++;
                acronymToId.putIfAbsent(acronym, conferenceId);
                w.write(conferenceId + ";" + csvEscape(confName) + ";" + csvEscape(acronym) + ";"
                        + csvEscape(confRank) + ";" + csvEscape(primaryFoR));
                w.newLine();
            }
            System.out.println("Wrote " + (nextId - 1) + " conferences");
        }
        return acronymToId;
    }

    private static String normalizeJournalTitle(String s) {
        if (s == null) return null;
        if (s.endsWith(",The")) {
            return "The " + s.substring(0, s.length() - ",The".length());
        }
        return s;
    }

    private static String onlyUpper(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) sb.append(c);
        }
        return sb.toString();
    }

    private static int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    private static List<String> parseCsvLine(String line, char delim) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == delim) {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        boolean needsQuote = s.indexOf(DELIM) >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!needsQuote) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
