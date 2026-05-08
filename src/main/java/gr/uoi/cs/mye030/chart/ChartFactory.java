package gr.uoi.cs.mye030.chart;

import gr.uoi.cs.mye030.service.ChartData.CategoryCount;
import gr.uoi.cs.mye030.service.ChartData.CountryCount;
import gr.uoi.cs.mye030.service.ChartData.YearCount;
import gr.uoi.cs.mye030.service.ChartData.YearlyAuthorCounts;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.spi.DefaultDataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;
import javafx.collections.ObservableList;

import java.util.List;

public final class ChartFactory {

    private ChartFactory() {}

    public static XYChart articlesPerYearChart(ObservableList<YearCount> data) {
        DoubleDataSet ds = DataSetAdapters.bindYearCount("Articles per Year", data);
        return buildChart("Year", "Articles", ds);
    }

    public static XYChart topAuthorsChart(ObservableList<CategoryCount> data) {
        DefaultDataSet ds = DataSetAdapters.bindCategoryCount("Top Authors", data);
        return buildChart("Author", "Articles", ds);
    }

    public static XYChart journalsByCountryChart(ObservableList<CountryCount> data) {
        DefaultDataSet ds = DataSetAdapters.bindCountryCount("Journals by Country", data);
        return buildChart("Country", "Journals", ds);
    }

    public static XYChart articlesByPublisherChart(ObservableList<CategoryCount> data) {
        DefaultDataSet ds = DataSetAdapters.bindCategoryCount("Articles by Publisher", data);
        return buildChart("Publisher", "Articles", ds);
    }

    public static XYChart conferencesByRankChart(ObservableList<CategoryCount> data) {
        DefaultDataSet ds = DataSetAdapters.bindCategoryCount("Conferences by Rank", data);
        return buildChart("Rank", "Conferences", ds);
    }

    public static XYChart articlesByJournalRankChart(ObservableList<CategoryCount> data) {
        DefaultDataSet ds = DataSetAdapters.bindCategoryCount("Articles by Journal Rank", data);
        return buildChart("Rank Bucket", "Articles", ds);
    }

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

    public static XYChart emptyGroupedBarChart(String xLabel, String yLabel) {
        DefaultNumericAxis xAxis = new DefaultNumericAxis(xLabel);
        DefaultNumericAxis yAxis = new DefaultNumericAxis(yLabel);
        XYChart chart = new XYChart(xAxis, yAxis);
        ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        renderer.setDrawBars(true);
        renderer.setShiftBar(true);
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
