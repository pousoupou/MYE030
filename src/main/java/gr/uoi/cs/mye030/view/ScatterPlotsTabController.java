package gr.uoi.cs.mye030.view;

import gr.uoi.cs.mye030.app.AppContext;
import gr.uoi.cs.mye030.chart.ChartFactory;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.service.QueryService;
import gr.uoi.cs.mye030.viewmodel.MainViewModel;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.spi.DefaultDataSet;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.concurrent.ExecutorService;

public final class ScatterPlotsTabController {

    @FXML private StackPane scatterChartContainer;

    private QueryService queryService;
    private ExecutorService background;
    private MainViewModel mainViewModel;

    private ErrorDataSetRenderer renderer;
    private FilterCriteria currentFilter = FilterCriteria.empty();

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();
        this.queryService = ctx.queryService();
        this.mainViewModel = ctx.mainViewModel();
        this.background = mainViewModel.background();

        XYChart chart = ChartFactory.emptyScatterChart("Total documents", "Total references");
        renderer = (ErrorDataSetRenderer) chart.getRenderers().get(0);
        scatterChartContainer.getChildren().add(chart);

        mainViewModel.registerFilterListener(this::onFilter);
        currentFilter = mainViewModel.lastFilter();
        reload();
    }

    private void onFilter(FilterCriteria f) {
        this.currentFilter = f == null ? FilterCriteria.empty() : f;
        Platform.runLater(this::reload);
    }

    private void reload() {
        FilterCriteria f = currentFilter;
        Task<List<Journal>> task = new Task<>() {
            @Override
            protected List<Journal> call() {
                return queryService.findJournals(f);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> applyJournals(task.getValue())));
        task.setOnFailed(e -> logTaskFailure("findJournals (scatterplot)", task.getException()));
        background.submit(task);
    }

    private void applyJournals(List<Journal> journals) {
        renderer.getDatasets().clear();
        if (journals == null || journals.isEmpty()) return;
        DefaultDataSet ds = new DefaultDataSet("Journals");
        for (Journal j : journals) {
            ds.add(j.totalDocs(), j.totalRefs(), j.title());
        }
        renderer.getDatasets().add(ds);
    }

    private static void logTaskFailure(String label, Throwable ex) {
        System.err.println(label + " failed: " + (ex == null ? "<no exception>" : ex.toString()));
        if (ex != null) ex.printStackTrace(System.err);
    }
}
