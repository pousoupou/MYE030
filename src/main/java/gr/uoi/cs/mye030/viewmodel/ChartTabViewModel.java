package gr.uoi.cs.mye030.viewmodel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class ChartTabViewModel<T> {

    private final StringProperty title = new SimpleStringProperty();
    private final ObservableList<T> data = FXCollections.observableArrayList();

    public ChartTabViewModel(String title) {
        this.title.set(title);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public ObservableList<T> getData() {
        return data;
    }
}
