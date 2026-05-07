package gr.uoi.cs.mye030.viewmodel;

import gr.uoi.cs.mye030.model.ArticleType;
import gr.uoi.cs.mye030.model.FilterCriteria;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.util.Set;

public final class FilterPaneViewModel {

    private final ObjectProperty<ArticleType> type = new SimpleObjectProperty<>();
    private final IntegerProperty yearFrom = new SimpleIntegerProperty();
    private final IntegerProperty yearTo = new SimpleIntegerProperty();
    private final ObservableSet<String> countries = FXCollections.observableSet();
    private final ObservableSet<String> ranks = FXCollections.observableSet();
    private final StringProperty publisher = new SimpleStringProperty();
    private final IntegerProperty topN = new SimpleIntegerProperty(10);

    public ObjectProperty<ArticleType> typeProperty() { return type; }
    public IntegerProperty yearFromProperty() { return yearFrom; }
    public IntegerProperty yearToProperty() { return yearTo; }
    public ObservableSet<String> getCountries() { return countries; }
    public ObservableSet<String> getRanks() { return ranks; }
    public StringProperty publisherProperty() { return publisher; }
    public IntegerProperty topNProperty() { return topN; }

    public FilterCriteria snapshot() {
        return new FilterCriteria(
                type.get(),
                yearFrom.get() == 0 ? null : yearFrom.get(),
                yearTo.get() == 0 ? null : yearTo.get(),
                Set.copyOf(countries),
                Set.copyOf(ranks),
                publisher.get(),
                topN.get() == 0 ? null : topN.get()
        );
    }
}
