package gr.uoi.cs.mye030.chart;

import gr.uoi.cs.mye030.service.ChartData.CategoryCount;
import gr.uoi.cs.mye030.service.ChartData.CountryCount;
import gr.uoi.cs.mye030.service.ChartData.YearCount;
import gr.uoi.cs.mye030.service.ChartData.YearlyAuthorCounts;
import io.fair_acc.dataset.spi.DefaultDataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.List;

public final class DataSetAdapters {

    private DataSetAdapters() {}

    public static DoubleDataSet bindYearCount(String name, ObservableList<YearCount> source) {
        DoubleDataSet ds = new DoubleDataSet(name);
        ListChangeListener<YearCount> listener = c -> rebuildYear(ds, source);
        source.addListener(listener);
        rebuildYear(ds, source);
        return ds;
    }

    private static void rebuildYear(DoubleDataSet ds, ObservableList<YearCount> source) {
        ds.clearData();
        for (YearCount yc : source) {
            ds.add(yc.year(), yc.count());
        }
    }

    public static DefaultDataSet bindCategoryCount(String name, ObservableList<CategoryCount> source) {
        DefaultDataSet ds = new DefaultDataSet(name);
        ListChangeListener<CategoryCount> listener = c -> rebuildCategory(ds, source);
        source.addListener(listener);
        rebuildCategory(ds, source);
        return ds;
    }

    private static void rebuildCategory(DefaultDataSet ds, ObservableList<CategoryCount> source) {
        ds.clearData();
        int i = 0;
        for (CategoryCount cc : source) {
            ds.add(i, cc.count(), cc.category());
            i++;
        }
    }

    public static DefaultDataSet bindCountryCount(String name, ObservableList<CountryCount> source) {
        DefaultDataSet ds = new DefaultDataSet(name);
        ListChangeListener<CountryCount> listener = c -> rebuildCountry(ds, source);
        source.addListener(listener);
        rebuildCountry(ds, source);
        return ds;
    }

    private static void rebuildCountry(DefaultDataSet ds, ObservableList<CountryCount> source) {
        ds.clearData();
        int i = 0;
        for (CountryCount cc : source) {
            ds.add(i, cc.count(), cc.country());
            i++;
        }
    }

    public static List<DoubleDataSet> bindYearlyAuthorCounts(ObservableList<YearlyAuthorCounts> source) {
        DoubleDataSet distinctDs = new DoubleDataSet("Distinct authors");
        DoubleDataSet totalDs = new DoubleDataSet("Total author-publications");
        ListChangeListener<YearlyAuthorCounts> listener = c -> rebuildYearlyAuthorCounts(distinctDs, totalDs, source);
        source.addListener(listener);
        rebuildYearlyAuthorCounts(distinctDs, totalDs, source);
        return List.of(distinctDs, totalDs);
    }

    private static void rebuildYearlyAuthorCounts(DoubleDataSet distinctDs,
                                                  DoubleDataSet totalDs,
                                                  ObservableList<YearlyAuthorCounts> source) {
        distinctDs.clearData();
        totalDs.clearData();
        for (YearlyAuthorCounts y : source) {
            distinctDs.add(y.year(), y.distinctAuthors());
            totalDs.add(y.year(), y.totalAuthors());
        }
    }
}
