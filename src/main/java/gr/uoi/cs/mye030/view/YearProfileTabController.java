package gr.uoi.cs.mye030.view;

import gr.uoi.cs.mye030.service.ChartData.YearProfileStats;
import gr.uoi.cs.mye030.service.ChartData.YearPublication;
import gr.uoi.cs.mye030.service.QueryService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public final class YearProfileTabController {

    @FXML private Label titleLabel;
    @FXML private GridPane statsGrid;
    @FXML private TabPane publicationsTabs;
    @FXML private TableView<YearPublication> journalTable;
    @FXML private TableColumn<YearPublication, String> journalTitleCol;
    @FXML private TableColumn<YearPublication, String> journalAuthorsCol;
    @FXML private TableView<YearPublication> conferenceTable;
    @FXML private TableColumn<YearPublication, String> conferenceTitleCol;
    @FXML private TableColumn<YearPublication, String> conferenceAuthorsCol;

    private final ObservableList<YearPublication> journalRows = FXCollections.observableArrayList();
    private final ObservableList<YearPublication> conferenceRows = FXCollections.observableArrayList();

    private QueryService queryService;
    private ExecutorService background;
    private int year;

    @FXML
    public void initialize() {
        journalTitleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().title()));
        journalAuthorsCol.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().authors())));
        conferenceTitleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().title()));
        conferenceAuthorsCol.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().authors())));
        journalTable.setItems(journalRows);
        conferenceTable.setItems(conferenceRows);
    }

    public void bind(int year, QueryService qs, ExecutorService background) {
        this.year = year;
        this.queryService = qs;
        this.background = background;

        titleLabel.setText("Year " + year);
        runStats(() -> qs.yearProfileStats(year));
        runInto(journalRows, () -> qs.journalPublicationsForYear(year));
        runInto(conferenceRows, () -> qs.conferencePublicationsForYear(year));
    }

    private void renderStats(YearProfileStats s) {
        statsGrid.getChildren().clear();
        if (s == null) return;
        addStat(0, "Total articles", String.valueOf(s.totalArticles()));
        addStat(1, "Discrete journals", String.valueOf(s.distinctJournals()));
        addStat(2, "Discrete conferences", String.valueOf(s.distinctConferences()));
        addStat(3, "Discrete authors", String.valueOf(s.distinctAuthors()));
        addStat(4, "Total authors (with duplicates)", String.valueOf(s.totalAuthors()));
    }

    private void addStat(int row, String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("profile-meta-key");
        Label v = new Label(value);
        v.getStyleClass().add("profile-meta-value");
        statsGrid.add(k, 0, row);
        statsGrid.add(v, 1, row);
    }

    private void runStats(Supplier<YearProfileStats> producer) {
        Task<YearProfileStats> task = new Task<>() {
            @Override
            protected YearProfileStats call() {
                return producer.get();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> renderStats(task.getValue())));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Year stats query failed: " + (ex == null ? "<no exception>" : ex.toString()));
            if (ex != null) ex.printStackTrace(System.err);
        });
        background.submit(task);
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
            System.err.println("Year publications query failed: " + (ex == null ? "<no exception>" : ex.toString()));
            if (ex != null) ex.printStackTrace(System.err);
        });
        background.submit(task);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
