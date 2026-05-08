package gr.uoi.cs.mye030.view;

import gr.uoi.cs.mye030.app.AppContext;
import gr.uoi.cs.mye030.chart.ChartFactory;
import gr.uoi.cs.mye030.model.Conference;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.service.ChartData.MultiYearSeries;
import gr.uoi.cs.mye030.service.ChartData.YearCount;
import gr.uoi.cs.mye030.service.QueryService;
import gr.uoi.cs.mye030.viewmodel.MainViewModel;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;
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

public final class LinechartsTabController {

    @FXML private ToggleGroup modeGroup;
    @FXML private ToggleButton journalsToggle;
    @FXML private ToggleButton conferencesToggle;

    @FXML private TextField entitySearchField;
    @FXML private ListView<EntityRow> entityListView;
    @FXML private StackPane entitiesChartContainer;

    @FXML private TextField categorySearchField;
    @FXML private ListView<CategoryRow> categoryListView;
    @FXML private StackPane categoriesChartContainer;

    private QueryService queryService;
    private ExecutorService background;
    private MainViewModel mainViewModel;

    private final ObservableList<EntityRow> allEntities = FXCollections.observableArrayList();
    private final FilteredList<EntityRow> visibleEntities = new FilteredList<>(allEntities, e -> true);

    private final ObservableList<CategoryRow> allCategories = FXCollections.observableArrayList();
    private final FilteredList<CategoryRow> visibleCategories = new FilteredList<>(allCategories, c -> true);

    private XYChart entitiesChart;
    private ErrorDataSetRenderer entitiesRenderer;
    private XYChart categoriesChart;
    private ErrorDataSetRenderer categoriesRenderer;

    private FilterCriteria currentFilter = FilterCriteria.empty();

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();
        this.queryService = ctx.queryService();
        this.mainViewModel = ctx.mainViewModel();
        this.background = mainViewModel.background();

        entitiesChart = ChartFactory.emptyLineChart("Year", "Articles");
        entitiesRenderer = (ErrorDataSetRenderer) entitiesChart.getRenderers().get(0);
        entitiesChartContainer.getChildren().add(entitiesChart);

        categoriesChart = ChartFactory.emptyLineChart("Year", "Active");
        categoriesRenderer = (ErrorDataSetRenderer) categoriesChart.getRenderers().get(0);
        categoriesChartContainer.getChildren().add(categoriesChart);

        setupEntityList();
        setupCategoryList();
        wireSearch();

        modeGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) {
                if (o != null) o.setSelected(true);
                return;
            }
            reloadForMode();
        });

        mainViewModel.registerFilterListener(this::onFilter);
        currentFilter = mainViewModel.lastFilter();
        reloadForMode();
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

    private void setupCategoryList() {
        categoryListView.setItems(visibleCategories);
        categoryListView.setCellFactory(CheckBoxListCell.forListView(
                CategoryRow::selectedProperty,
                new StringConverter<>() {
                    @Override public String toString(CategoryRow row) { return row == null ? "" : row.name(); }
                    @Override public CategoryRow fromString(String s) { return null; }
                }));
    }

    private void wireSearch() {
        entitySearchField.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty()) visibleEntities.setPredicate(e -> true);
            else visibleEntities.setPredicate(e -> e.name().toLowerCase(Locale.ROOT).contains(q));
        });
        categorySearchField.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty()) visibleCategories.setPredicate(c -> true);
            else visibleCategories.setPredicate(c -> c.name().toLowerCase(Locale.ROOT).contains(q));
        });
    }

    private boolean isJournalsMode() {
        return modeGroup.getSelectedToggle() == journalsToggle;
    }

    private void reloadForMode() {
        loadEntities();
        loadCategories();
    }

    private void loadEntities() {
        boolean journalsMode = isJournalsMode();
        Supplier<List<EntityRow>> producer = journalsMode
                ? () -> {
                    List<Journal> js = queryService.findJournals(FilterCriteria.empty());
                    List<EntityRow> rows = new ArrayList<>(js.size());
                    for (Journal j : js) {
                        EntityRow r = new EntityRow(j.id(), j.title());
                        r.selectedProperty().addListener((obs, o, n) -> redrawEntitiesChart());
                        rows.add(r);
                    }
                    return rows;
                }
                : () -> {
                    List<Conference> cs = queryService.findConferences(FilterCriteria.empty());
                    List<EntityRow> rows = new ArrayList<>(cs.size());
                    for (Conference c : cs) {
                        EntityRow r = new EntityRow(c.id(), c.name());
                        r.selectedProperty().addListener((obs, o, n) -> redrawEntitiesChart());
                        rows.add(r);
                    }
                    return rows;
                };
        runInto(allEntities, producer, this::redrawEntitiesChart);
    }

    private void loadCategories() {
        boolean journalsMode = isJournalsMode();
        Supplier<List<CategoryRow>> producer = () -> {
            List<String> raw = journalsMode
                    ? queryService.distinctSubjectAreas()
                    : queryService.distinctPrimaryFoRs();
            List<CategoryRow> rows = new ArrayList<>(raw.size());
            for (String s : raw) {
                CategoryRow r = new CategoryRow(s);
                r.selectedProperty().addListener((obs, o, n) -> redrawCategoriesChart());
                rows.add(r);
            }
            return rows;
        };
        runInto(allCategories, producer, this::redrawCategoriesChart);
    }

    private void onFilter(FilterCriteria f) {
        this.currentFilter = f == null ? FilterCriteria.empty() : f;
        Platform.runLater(() -> {
            redrawEntitiesChart();
            redrawCategoriesChart();
        });
    }

    private FilterCriteria filterForQuery() {
        FilterCriteria f = currentFilter == null ? FilterCriteria.empty() : currentFilter;
        return new FilterCriteria(null, f.yearFrom(), f.yearTo(), f.countries(), f.ranks(), f.publisher(), f.topN());
    }

    private void redrawEntitiesChart() {
        Set<Integer> ids = new LinkedHashSet<>();
        for (EntityRow r : allEntities) if (r.selectedProperty().get()) ids.add(r.id());
        if (ids.isEmpty()) {
            entitiesRenderer.getDatasets().clear();
            return;
        }
        boolean journalsMode = isJournalsMode();
        FilterCriteria f = filterForQuery();
        Task<List<MultiYearSeries>> task = new Task<>() {
            @Override
            protected List<MultiYearSeries> call() {
                return journalsMode
                        ? queryService.articlesPerYearForJournals(ids, f)
                        : queryService.articlesPerYearForConferences(ids, f);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> applySeries(entitiesRenderer, task.getValue())));
        task.setOnFailed(e -> logTaskFailure("articlesPerYear (linechart)", task.getException()));
        background.submit(task);
    }

    private void redrawCategoriesChart() {
        Set<String> cats = new LinkedHashSet<>();
        for (CategoryRow r : allCategories) if (r.selectedProperty().get()) cats.add(r.name());
        if (cats.isEmpty()) {
            categoriesRenderer.getDatasets().clear();
            return;
        }
        boolean journalsMode = isJournalsMode();
        FilterCriteria f = filterForQuery();
        Task<List<MultiYearSeries>> task = new Task<>() {
            @Override
            protected List<MultiYearSeries> call() {
                return journalsMode
                        ? queryService.activeJournalsBySubjectAreaPerYear(cats, f)
                        : queryService.activeConferencesByPrimaryForPerYear(cats, f);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> applySeries(categoriesRenderer, task.getValue())));
        task.setOnFailed(e -> logTaskFailure("activeByCategory (linechart)", task.getException()));
        background.submit(task);
    }

    private void applySeries(ErrorDataSetRenderer renderer, List<MultiYearSeries> seriesList) {
        renderer.getDatasets().clear();
        if (seriesList == null) return;
        List<DataSet> datasets = new ArrayList<>(seriesList.size());
        for (MultiYearSeries s : seriesList) {
            DoubleDataSet ds = new DoubleDataSet(s.name());
            for (YearCount yc : s.points()) ds.add(yc.year(), yc.count());
            datasets.add(ds);
        }
        renderer.getDatasets().addAll(datasets);
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
        task.setOnFailed(e -> logTaskFailure("linecharts list query", task.getException()));
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

    static final class CategoryRow {
        private final String name;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        CategoryRow(String name) { this.name = name; }
        String name() { return name; }
        BooleanProperty selectedProperty() { return selected; }
    }
}
