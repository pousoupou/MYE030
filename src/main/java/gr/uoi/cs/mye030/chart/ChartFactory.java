package gr.uoi.cs.mye030.chart;

import gr.uoi.cs.mye030.service.ChartData.YearCount;
import gr.uoi.cs.mye030.service.ChartData.YearlyAuthorCounts;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.spi.DoubleDataSet;
import javafx.collections.ObservableList;

import java.util.List;

public final class ChartFactory {

    private ChartFactory() {}

    public static XYChart profileArticlesPerYearChart(ObservableList<YearCount> data) {
        DoubleDataSet ds = DataSetAdapters.bindYearCount("Articles", data);
        return buildChart("Year", "Articles", ds);
    }

    public static XYChart profileAuthorsPerYearChart(ObservableList<YearlyAuthorCounts> data) {
        List<DoubleDataSet> sets = DataSetAdapters.bindYearlyAuthorCounts(data);
        return buildChart("Year", "Authors", sets.toArray(new io.fair_acc.dataset.DataSet[0]));
    }

    public static XYChart emptyLineChart(String xLabel, String yLabel) {
        DefaultNumericAxis xAxis = new DefaultNumericAxis(xLabel);
        DefaultNumericAxis yAxis = new DefaultNumericAxis(yLabel);
        XYChart chart = new XYChart(xAxis, yAxis);
        chart.getRenderers().setAll(new ErrorDataSetRenderer());
        return chart;
    }

    public static XYChart emptyCategoryBarChart(String xLabel, String yLabel) {
        DefaultNumericAxis xAxis = new DefaultNumericAxis(xLabel);
        xAxis.setMinorTickCount(0);
        xAxis.setAutoRanging(false);
        DefaultNumericAxis yAxis = new DefaultNumericAxis(yLabel);
        yAxis.setForceZeroInRange(true);
        XYChart chart = new XYChart(xAxis, yAxis);
        chart.setLegendVisible(false);
        ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        renderer.setDrawBars(true);
        renderer.setShiftBar(false);
        renderer.setDynamicBarWidth(false);
        renderer.setBarWidth(24);
        renderer.setDrawMarker(false);
        renderer.setPolyLineStyle(LineStyle.NONE);
        chart.getRenderers().setAll(renderer);
        return chart;
    }

    public static XYChart emptyGroupedBarChart(String xLabel, String yLabel) {
        DefaultNumericAxis xAxis = new DefaultNumericAxis(xLabel);
        xAxis.setMinorTickCount(0);
        xAxis.setAutoRanging(false);
        DefaultNumericAxis yAxis = new DefaultNumericAxis(yLabel);
        yAxis.setForceZeroInRange(true);
        XYChart chart = new XYChart(xAxis, yAxis);
        ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        renderer.setDrawBars(true);
        renderer.setShiftBar(true);
        renderer.setshiftBarOffset(14);
        renderer.setDynamicBarWidth(false);
        renderer.setBarWidth(12);
        renderer.setDrawMarker(false);
        renderer.setPolyLineStyle(LineStyle.NONE);
        chart.getRenderers().setAll(renderer);
        return chart;
    }

    private static XYChart buildChart(String xLabel, String yLabel, io.fair_acc.dataset.DataSet... datasets) {
        DefaultNumericAxis xAxis = new DefaultNumericAxis(xLabel);
        DefaultNumericAxis yAxis = new DefaultNumericAxis(yLabel);
        XYChart chart = new XYChart(xAxis, yAxis);
        ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        for (io.fair_acc.dataset.DataSet ds : datasets) renderer.getDatasets().add(ds);
        chart.getRenderers().setAll(renderer);
        return chart;
    }
}
