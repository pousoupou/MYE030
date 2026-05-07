package gr.uoi.cs.mye030.view;

import gr.uoi.cs.mye030.chart.ChartFactory;
import gr.uoi.cs.mye030.model.Author;
import gr.uoi.cs.mye030.service.ChartData.AuthorProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearCount;
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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public final class AuthorProfileTabController {

    @FXML private Label titleLabel;
    @FXML private GridPane statsGrid;
    @FXML private StackPane articlesChartContainer;

    private final ObservableList<YearCount> articles = FXCollections.observableArrayList();

    private QueryService queryService;
    private ExecutorService background;

    @FXML
    public void initialize() {
        articlesChartContainer.getChildren().add(ChartFactory.profileArticlesPerYearChart(articles));
    }

    public void bind(Author a, QueryService qs, ExecutorService background) {
        this.queryService = qs;
        this.background = background;

        titleLabel.setText(a.name());
        runInto(articles, () -> qs.articlesPerYearForAuthor(a.id()));
        runStats(() -> qs.authorProfileStats(a.id()));
    }

    private void renderStats(AuthorProfileStats s) {
        statsGrid.getChildren().clear();
        if (s == null) return;
        String firstYear = s.firstYear() == null ? "—" : String.valueOf(s.firstYear());
        String lastYear  = s.lastYear()  == null ? "—" : String.valueOf(s.lastYear());
        addStat(0, "First known year", firstYear);
        addStat(1, "Last known year", lastYear);
        addStat(2, "Total articles", String.valueOf(s.totalArticles()));
        addStat(3, "Avg articles per year", formatAvg(s.avgArticlesPerYear()));
    }

    private void addStat(int row, String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("profile-meta-key");
        Label v = new Label(value);
        v.getStyleClass().add("profile-meta-value");
        statsGrid.add(k, 0, row);
        statsGrid.add(v, 1, row);
    }

    private <T> void runInto(ObservableList<T> target, Supplier<List<T>> producer) {
        Task<List<T>> task = new Task<>() {
            @Override
            protected List<T> call() {
                return producer.get();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> target.setAll(task.getValue())));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Author profile query failed: " + (ex == null ? "<no exception>" : ex.toString()));
            if (ex != null) ex.printStackTrace(System.err);
        });
        background.submit(task);
    }

    private void runStats(Supplier<AuthorProfileStats> producer) {
        Task<AuthorProfileStats> task = new Task<>() {
            @Override
            protected AuthorProfileStats call() {
                return producer.get();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> renderStats(task.getValue())));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Author profile stats query failed: " + (ex == null ? "<no exception>" : ex.toString()));
            if (ex != null) ex.printStackTrace(System.err);
        });
        background.submit(task);
    }

    private static String formatAvg(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
