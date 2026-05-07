package gr.uoi.cs.mye030.view;

import gr.uoi.cs.mye030.chart.ChartFactory;
import gr.uoi.cs.mye030.model.Conference;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.service.ChartData.ProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearCount;
import gr.uoi.cs.mye030.service.ChartData.YearlyAuthorCounts;
import gr.uoi.cs.mye030.service.QueryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ProfileTabController {

    @FXML private Label titleLabel;
    @FXML private GridPane metaGrid;
    @FXML private GridPane statsGrid;
    @FXML private StackPane articlesChartContainer;
    @FXML private StackPane authorsChartContainer;

    private final ObservableList<YearCount> articles = FXCollections.observableArrayList();
    private final ObservableList<YearlyAuthorCounts> authors = FXCollections.observableArrayList();

    private QueryService queryService;
    private ExecutorService background;
    private Function<FilterCriteria, List<YearCount>> articlesFetcher;
    private Function<FilterCriteria, List<YearlyAuthorCounts>> authorsFetcher;
    private Function<FilterCriteria, ProfileStats> statsFetcher;

    @FXML
    public void initialize() {
        articlesChartContainer.getChildren().add(ChartFactory.profileArticlesPerYearChart(articles));
        authorsChartContainer.getChildren().add(ChartFactory.profileAuthorsPerYearChart(authors));
    }

    public void bindJournal(Journal j, QueryService qs, ExecutorService background, FilterCriteria initial) {
        this.queryService = qs;
        this.background = background;
        this.articlesFetcher = f -> qs.articlesPerYearForJournal(j.id(), f);
        this.authorsFetcher = f -> qs.authorsPerYearForJournal(j.id(), f);
        this.statsFetcher = f -> qs.profileStatsForJournal(j.id(), f);

        titleLabel.setText(j.title());
        addMeta(0, "Acronym", j.acronym());
        addMeta(1, "Rank", String.valueOf(j.rank()));
        addMeta(2, "Country", j.country());
        addMeta(3, "Subject", j.bestSubjectArea());
        refresh(initial);
    }

    public void bindConference(Conference c, QueryService qs, ExecutorService background, FilterCriteria initial) {
        this.queryService = qs;
        this.background = background;
        this.articlesFetcher = f -> qs.articlesPerYearForConference(c.id(), f);
        this.authorsFetcher = f -> qs.authorsPerYearForConference(c.id(), f);
        this.statsFetcher = f -> qs.profileStatsForConference(c.id(), f);

        titleLabel.setText(c.name());
        addMeta(0, "Acronym", c.acronym());
        addMeta(1, "Rank", c.rank());
        addMeta(2, "Primary FoR", c.primaryFoR());
        refresh(initial);
    }

    public void refresh(FilterCriteria f) {
        if (queryService == null) return;
        runInto(articles, () -> articlesFetcher.apply(f));
        runInto(authors, () -> authorsFetcher.apply(f));
        runStats(() -> statsFetcher.apply(f));
    }

    private void addMeta(int row, String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("profile-meta-key");
        Label v = new Label(value == null ? "—" : value);
        v.getStyleClass().add("profile-meta-value");
        metaGrid.add(k, 0, row);
        metaGrid.add(v, 1, row);
    }

    private <T> void runInto(ObservableList<T> target, java.util.function.Supplier<List<T>> producer) {
        Task<List<T>> task = new Task<>() {
            @Override
            protected List<T> call() {
                return producer.get();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> target.setAll(task.getValue())));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Profile query failed: " + (ex == null ? "<no exception>" : ex.toString()));
            if (ex != null) ex.printStackTrace(System.err);
        });
        background.submit(task);
    }

    private void runStats(Supplier<ProfileStats> producer) {
        Task<ProfileStats> task = new Task<>() {
            @Override
            protected ProfileStats call() {
                return producer.get();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> renderStats(task.getValue())));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Profile stats query failed: " + (ex == null ? "<no exception>" : ex.toString()));
            if (ex != null) ex.printStackTrace(System.err);
        });
        background.submit(task);
    }

    private void renderStats(ProfileStats s) {
        statsGrid.getChildren().clear();
        if (s == null) return;
        String firstYear = s.firstYear() == null ? "—" : String.valueOf(s.firstYear());
        String lastYear = s.lastYear() == null ? "—" : String.valueOf(s.lastYear());
        addStat(0, "First known year", firstYear);
        addStat(1, "Last known year", lastYear);
        addStat(2, "Total articles", String.valueOf(s.totalArticles()));
        addStat(3, "Authors who published", String.valueOf(s.distinctAuthors()));
        addStat(4, "Total authors (with duplicates)", String.valueOf(s.totalAuthors()));
        addStat(5, "Avg authors per article", formatAvg(s.avgAuthorsPerArticle()));
        addStat(6, "Avg articles per year", formatAvg(s.avgArticlesPerYear()));
        addStat(7, "Avg authors per year", formatAvg(s.avgAuthorsPerYear()));
    }

    private void addStat(int row, String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("profile-meta-key");
        Label v = new Label(value);
        v.getStyleClass().add("profile-meta-value");
        statsGrid.add(k, 0, row);
        statsGrid.add(v, 1, row);
    }

    private static String formatAvg(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}
