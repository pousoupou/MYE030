package gr.uoi.cs.mye030.view;

import gr.uoi.cs.mye030.model.ArticleType;
import gr.uoi.cs.mye030.service.ChartData.AuthorListRow;
import gr.uoi.cs.mye030.viewmodel.FilterPaneViewModel;
import gr.uoi.cs.mye030.viewmodel.MainViewModel;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

import java.util.Locale;

public final class FilterPaneController {

    @FXML private ChoiceBox<ArticleType> typeChoice;
    @FXML private Spinner<Integer> yearFromSpinner;
    @FXML private Spinner<Integer> yearToSpinner;
    @FXML private Spinner<Integer> yearProfileSpinner;
    @FXML private Button applyButton;
    @FXML private Button openYearProfileButton;
    @FXML private TextField authorSearchField;
    @FXML private Button openAuthorButton;

    private FilterPaneViewModel viewModel;
    private MainViewModel mainViewModel;

    @FXML
    public void initialize() {
        typeChoice.setItems(FXCollections.observableArrayList(ArticleType.values()));
        yearFromSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 2100, 0));
        yearToSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 2100, 0));
        yearProfileSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 2100, 0));

        authorSearchField.setOnAction(ev -> openMatchingAuthor());
    }

    public void bind(FilterPaneViewModel viewModel, MainViewModel mainViewModel) {
        this.viewModel = viewModel;
        this.mainViewModel = mainViewModel;

        typeChoice.valueProperty().bindBidirectional(viewModel.typeProperty());

        bindSpinner(yearFromSpinner, viewModel, FilterPaneViewModel::yearFromProperty);
        bindSpinner(yearToSpinner, viewModel, FilterPaneViewModel::yearToProperty);
    }

    private static void bindSpinner(Spinner<Integer> spinner,
                                    FilterPaneViewModel vm,
                                    java.util.function.Function<FilterPaneViewModel, javafx.beans.property.IntegerProperty> prop) {
        javafx.beans.property.IntegerProperty p = prop.apply(vm);
        spinner.getValueFactory().setValue(p.get());
        ChangeListener<Number> fromVm = (obs, o, n) -> spinner.getValueFactory().setValue(n.intValue());
        ChangeListener<Integer> fromUi = (obs, o, n) -> p.set(n == null ? 0 : n);
        p.addListener(fromVm);
        spinner.valueProperty().addListener(fromUi);
    }

    @FXML
    private void onApply() {
        if (mainViewModel != null && viewModel != null) {
            mainViewModel.refresh(viewModel.snapshot());
        }
    }

    @FXML
    private void onOpenYearProfile() {
        if (mainViewModel == null || yearProfileSpinner == null) return;
        Integer year = yearProfileSpinner.getValue();
        if (year == null || year <= 0) return;
        mainViewModel.openYearProfile(year);
    }

    @FXML
    private void onOpenAuthor() {
        openMatchingAuthor();
    }

    private void openMatchingAuthor() {
        if (mainViewModel == null) return;
        String raw = authorSearchField.getText();
        if (raw == null) return;
        String q = raw.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return;

        AuthorListRow exact = null;
        AuthorListRow prefix = null;
        AuthorListRow contains = null;
        for (AuthorListRow row : mainViewModel.authors()) {
            String name = row.author().name();
            if (name == null) continue;
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals(q)) {
                exact = row;
                break;
            }
            if (prefix == null && lower.startsWith(q)) prefix = row;
            else if (contains == null && lower.contains(q)) contains = row;
        }
        AuthorListRow match = exact != null ? exact : (prefix != null ? prefix : contains);
        if (match != null) mainViewModel.openAuthorProfile(match.author());
    }
}
