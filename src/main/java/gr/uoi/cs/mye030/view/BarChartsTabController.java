package gr.uoi.cs.mye030.view;

import gr.uoi.cs.mye030.app.AppContext;
import gr.uoi.cs.mye030.chart.ChartFactory;
import gr.uoi.cs.mye030.model.Conference;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.service.ChartData.EntityBarStats;
import gr.uoi.cs.mye030.service.ChartData.PublisherJournalQuarters;
import gr.uoi.cs.mye030.service.QueryService;
import gr.uoi.cs.mye030.viewmodel.MainViewModel;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DefaultDataSet;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public final class BarChartsTabController {

    @FXML private ToggleGroup modeGroup;
    @FXML private ToggleButton journalsToggle;
    @FXML private ToggleButton conferencesToggle;

    @FXML private TextField entitySearchField;
    @FXML private ListView<EntityRow> entityListView;
    @FXML private StackPane entityStatsChartContainer;

    @FXML private TextField publisherSearchField;
    @FXML private ListView<PublisherRow> publisherListView;
    @FXML private StackPane publisherChartContainer;

    private QueryService queryService;
    private ExecutorService background;
    private MainViewModel mainViewModel;

    private final ObservableList<EntityRow> allEntities = FXCollections.observableArrayList();
    private final FilteredList<EntityRow> visibleEntities = new FilteredList<>(allEntities, e -> true);

    private final ObservableList<PublisherRow> allPublishers = FXCollections.observableArrayList();
    private final FilteredList<PublisherRow> visiblePublishers = new FilteredList<>(allPublishers, p -> true);

    private XYChart entityStatsChart;
    private ErrorDataSetRenderer entityStatsRenderer;
    private XYChart publisherChart;
    private ErrorDataSetRenderer publisherRenderer;

    private FilterCriteria currentFilter = FilterCriteria.empty();

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();
        this.queryService = ctx.queryService();
        this.mainViewModel = ctx.mainViewModel();
        this.background = mainViewModel.background();

        entityStatsChart = ChartFactory.emptyGroupedBarChart("Entity", "Value");
        entityStatsRenderer = (ErrorDataSetRenderer) entityStatsChart.getRenderers().get(0);
        entityStatsChartContainer.getChildren().add(entityStatsChart);

        publisherChart = ChartFactory.emptyGroupedBarChart("Publisher", "Distinct journals");
        publisherRenderer = (ErrorDataSetRenderer) publisherChart.getRenderers().get(0);
        publisherChartContainer.getChildren().add(publisherChart);

        setupEntityList();
        setupPublisherList();
        wireSearch();

        modeGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) {
                if (o != null) o.setSelected(true);
                return;
            }
            loadEntities();
        });

        mainViewModel.registerFilterListener(this::onFilter);
        currentFilter = mainViewModel.lastFilter();
        loadEntities();
        loadPublishers();
    }

    private void setupEntityList() {
        entityListView.setItems(visibleEntities);
        entityListView.setCellFactory(CheckBoxListCell.forListView(
                EntityRow::selectedProperty,
                new StringConverter<>() {
                    @Override public String toString(EntityRow row) { return row == null ? "" : row.name(); }
                    @Override public EntityRow fromString(String s) { return null; }
                }));
    }

    private void setupPublisherList() {
        publisherListView.setItems(visiblePublishers);
        publisherListView.setCellFactory(CheckBoxListCell.forListView(
                PublisherRow::selectedProperty,
                new StringConverter<>() {
                    @Override public String toString(PublisherRow row) { return row == null ? "" : row.name(); }
                    @Override public PublisherRow fromString(String s) { return null; }
                }));
    }

    private void wireSearch() {
        entitySearchField.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.trim().toLowerCase(Locale.ROOT);
            visibleEntities.setPredicate(q.isEmpty() ? e -> true : e -> e.name().toLowerCase(Locale.ROOT).contains(q));
        });
        publisherSearchField.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.trim().toLowerCase(Locale.ROOT);
            visiblePublishers.setPredicate(q.isEmpty() ? p -> true : p -> p.name().toLowerCase(Locale.ROOT).contains(q));
        });
    }

    private boolean isJournalsMode() {
        return modeGroup.getSelectedToggle() == journalsToggle;
    }

    private void loadEntities() {
        boolean journalsMode = isJournalsMode();
        Supplier<List<EntityRow>> producer = journalsMode
                ? () -> {
                    List<Journal> js = queryService.findJournals(FilterCriteria.empty());
                    List<EntityRow> rows = new ArrayList<>(js.size());
                    for (Journal j : js) {
                        EntityRow r = new EntityRow(j.id(), j.title());
                        r.selectedProperty().addListener((obs, o, n) -> redrawEntityStatsChart());
                        rows.add(r);
                    }
                    return rows;
                }
                : () -> {
                    List<Conference> cs = queryService.findConferences(FilterCriteria.empty());
                    List<EntityRow> rows = new ArrayList<>(cs.size());
                    for (Conference c : cs) {
                        EntityRow r = new EntityRow(c.id(), c.name());
                        r.selectedProperty().addListener((obs, o, n) -> redrawEntityStatsChart());
                        rows.add(r);
                    }
                    return rows;
                };
        runInto(allEntities, producer, this::redrawEntityStatsChart);
    }

    private void loadPublishers() {
        runInto(allPublishers, () -> {
            List<String> raw = queryService.distinctJournalPublishers();
            List<PublisherRow> rows = new ArrayList<>(raw.size());
            for (String s : raw) {
                PublisherRow r = new PublisherRow(s);
                r.selectedProperty().addListener((obs, o, n) -> redrawPublisherChart());
                rows.add(r);
            }
            return rows;
        }, this::redrawPublisherChart);
    }

    private void onFilter(FilterCriteria f) {
        this.currentFilter = f == null ? FilterCriteria.empty() : f;
        Platform.runLater(() -> {
            redrawEntityStatsChart();
            redrawPublisherChart();
        });
    }

    private FilterCriteria filterForEntityQuery() {
        FilterCriteria f = currentFilter == null ? FilterCriteria.empty() : currentFilter;
        return new FilterCriteria(null, f.yearFrom(), f.yearTo(), f.countries(), f.ranks(), f.publisher(), f.topN());
    }

    private FilterCriteria filterForPublisherQuery() {
        FilterCriteria f = currentFilter == null ? FilterCriteria.empty() : currentFilter;
        return new FilterCriteria(null, f.yearFrom(), f.yearTo(), null, null, null, null);
    }

    private void redrawEntityStatsChart() {
        Set<Integer> ids = new LinkedHashSet<>();
        for (EntityRow r : allEntities) if (r.selectedProperty().get()) ids.add(r.id());
        if (ids.isEmpty()) {
            entityStatsRenderer.getDatasets().clear();
            return;
        }
        boolean journalsMode = isJournalsMode();
        FilterCriteria f = filterForEntityQuery();
        Task<List<EntityBarStats>> task = new Task<>() {
            @Override
            protected List<EntityBarStats> call() {
                return journalsMode
                        ? queryService.barStatsForJournals(ids, f)
                        : queryService.barStatsForConferences(ids, f);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> applyEntityStats(task.getValue())));
        task.setOnFailed(e -> logTaskFailure("barStats (barchart)", task.getException()));
        background.submit(task);
    }

    private void redrawPublisherChart() {
        Set<String> selected = new LinkedHashSet<>();
        for (PublisherRow r : allPublishers) if (r.selectedProperty().get()) selected.add(r.name());
        if (selected.isEmpty()) {
            publisherRenderer.getDatasets().clear();
            return;
        }
        FilterCriteria f = filterForPublisherQuery();
        Task<List<PublisherJournalQuarters>> task = new Task<>() {
            @Override
            protected List<PublisherJournalQuarters> call() {
                return queryService.journalQuartersByPublisher(selected, f);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> applyPublisherStats(task.getValue())));
        task.setOnFailed(e -> logTaskFailure("publisher quarters (barchart)", task.getException()));
        background.submit(task);
    }

    private void applyEntityStats(List<EntityBarStats> rows) {
        entityStatsRenderer.getDatasets().clear();
        if (rows == null || rows.isEmpty()) return;
        DefaultDataSet total = new DefaultDataSet("Total articles");
        DefaultDataSet avgArt = new DefaultDataSet("Avg articles/yr");
        DefaultDataSet avgAuth = new DefaultDataSet("Avg authors/yr");
        for (int i = 0; i < rows.size(); i++) {
            EntityBarStats r = rows.get(i);
            total.add(i, r.totalArticles(), r.name());
            avgArt.add(i, r.avgArticlesPerYear(), r.name());
            avgAuth.add(i, r.avgAuthorsPerYear(), r.name());
        }
        List<DataSet> sets = new ArrayList<>();
        sets.add(total); sets.add(avgArt); sets.add(avgAuth);
        entityStatsRenderer.getDatasets().addAll(sets);
    }

    private void applyPublisherStats(List<PublisherJournalQuarters> rows) {
        publisherRenderer.getDatasets().clear();
        if (rows == null || rows.isEmpty()) return;
        DefaultDataSet total = new DefaultDataSet("Total");
        DefaultDataSet q1 = new DefaultDataSet("Q1");
        DefaultDataSet q2 = new DefaultDataSet("Q2");
        DefaultDataSet q3 = new DefaultDataSet("Q3");
        DefaultDataSet q4 = new DefaultDataSet("Q4");
        for (int i = 0; i < rows.size(); i++) {
            PublisherJournalQuarters r = rows.get(i);
            total.add(i, r.total(), r.publisher());
            q1.add(i, r.q1(), r.publisher());
            q2.add(i, r.q2(), r.publisher());
            q3.add(i, r.q3(), r.publisher());
            q4.add(i, r.q4(), r.publisher());
        }
        List<DataSet> sets = new ArrayList<>();
        sets.add(total); sets.add(q1); sets.add(q2); sets.add(q3); sets.add(q4);
        publisherRenderer.getDatasets().addAll(sets);
    }

    private <T> void runInto(ObservableList<T> target, Supplier<List<T>> producer, Runnable afterApply) {
        Task<List<T>> task = new Task<>() {
            @Override
            protected List<T> call() {
                return producer.get();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            target.setAll(task.getValue());
            if (afterApply != null) afterApply.run();
        }));
        task.setOnFailed(e -> logTaskFailure("barcharts list query", task.getException()));
        background.submit(task);
    }

    private static void logTaskFailure(String label, Throwable ex) {
        System.err.println(label + " failed: " + (ex == null ? "<no exception>" : ex.toString()));
        if (ex != null) ex.printStackTrace(System.err);
    }

    static final class EntityRow {
        private final int id;
        private final String name;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        EntityRow(int id, String name) { this.id = id; this.name = name; }
        int id() { return id; }
        String name() { return name; }
        BooleanProperty selectedProperty() { return selected; }
    }

    static final class PublisherRow {
        private final String name;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        PublisherRow(String name) { this.name = name; }
        String name() { return name; }
        BooleanProperty selectedProperty() { return selected; }
    }
}
