package gr.uoi.cs.mye030.view;

import gr.uoi.cs.mye030.app.AppContext;
import gr.uoi.cs.mye030.model.Author;
import gr.uoi.cs.mye030.model.Conference;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.service.ChartData.AuthorListRow;
import gr.uoi.cs.mye030.viewmodel.MainViewModel;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class MainController {

    @FXML private TabPane tabPane;

    @FXML private TableView<Journal> journalsTable;
    @FXML private TableColumn<Journal, Number> journalRankCol;
    @FXML private TableColumn<Journal, String> journalTitleCol;
    @FXML private TableColumn<Journal, String> journalAcronymCol;
    @FXML private TableColumn<Journal, String> journalCountryCol;
    @FXML private TableColumn<Journal, String> journalSubjectCol;

    @FXML private TableView<Conference> conferencesTable;
    @FXML private TableColumn<Conference, String> confRankCol;
    @FXML private TableColumn<Conference, String> confNameCol;
    @FXML private TableColumn<Conference, String> confAcronymCol;
    @FXML private TableColumn<Conference, String> confForCol;

    @FXML private TableView<AuthorListRow> authorsTable;
    @FXML private TableColumn<AuthorListRow, String> authorNameCol;
    @FXML private TableColumn<AuthorListRow, Number> authorArticleCountCol;

    @FXML private FilterPaneController filterPaneController;

    private MainViewModel viewModel;
    private final Map<String, Tab> profileTabs = new HashMap<>();

    @FXML
    public void initialize() {
        AppContext ctx = AppContext.get();
        viewModel = ctx.mainViewModel();
        filterPaneController.bind(ctx.filterPaneViewModel(), viewModel);
        viewModel.setYearProfileOpener(this::openYearProfile);

        wireJournalsTable();
        wireConferencesTable();
        wireAuthorsTable();
        viewModel.loadAuthors();
    }

    private void wireJournalsTable() {
        journalRankCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().rank()));
        journalTitleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().title()));
        journalAcronymCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().acronym()));
        journalCountryCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().country()));
        journalSubjectCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().bestSubjectArea()));
        journalsTable.setItems(viewModel.journals());
        journalsTable.setRowFactory(tv -> {
            TableRow<Journal> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) openJournalProfile(row.getItem());
            });
            return row;
        });
    }

    private void wireConferencesTable() {
        confRankCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().rank()));
        confNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        confAcronymCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().acronym()));
        confForCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().primaryFoR()));
        conferencesTable.setItems(viewModel.conferences());
        conferencesTable.setRowFactory(tv -> {
            TableRow<Conference> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) openConferenceProfile(row.getItem());
            });
            return row;
        });
    }

    private void wireAuthorsTable() {
        authorNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().author().name()));
        authorArticleCountCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().articleCount()));
        authorsTable.setItems(viewModel.authors());
        authorsTable.setRowFactory(tv -> {
            TableRow<AuthorListRow> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) openAuthorProfile(row.getItem().author());
            });
            return row;
        });
    }

    private void openJournalProfile(Journal j) {
        String key = "J:" + j.id();
        Tab existing = profileTabs.get(key);
        if (existing != null) {
            tabPane.getSelectionModel().select(existing);
            return;
        }
        openProfileTab(key, j.title(), (ctrl, initial) ->
                ctrl.bindJournal(j, viewModel.queryService(), viewModel.background(), initial));
    }

    private void openConferenceProfile(Conference c) {
        String key = "C:" + c.id();
        Tab existing = profileTabs.get(key);
        if (existing != null) {
            tabPane.getSelectionModel().select(existing);
            return;
        }
        openProfileTab(key, c.name(), (ctrl, initial) ->
                ctrl.bindConference(c, viewModel.queryService(), viewModel.background(), initial));
    }

    private void openYearProfile(int year) {
        String key = "Y:" + year;
        Tab existing = profileTabs.get(key);
        if (existing != null) {
            tabPane.getSelectionModel().select(existing);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gr/uoi/cs/mye030/view/YearProfileTab.fxml"));
            Parent root = loader.load();
            YearProfileTabController ctrl = loader.getController();
            ctrl.bind(year, viewModel.queryService(), viewModel.background());

            Tab tab = new Tab("Year " + year);
            tab.setContent(root);
            tab.setOnClosed(e -> profileTabs.remove(key));
            profileTabs.put(key, tab);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
        } catch (Exception e) {
            System.err.println("Failed to open year profile tab: " + e);
            e.printStackTrace(System.err);
        }
    }

    private void openAuthorProfile(Author a) {
        String key = "A:" + a.id();
        Tab existing = profileTabs.get(key);
        if (existing != null) {
            tabPane.getSelectionModel().select(existing);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gr/uoi/cs/mye030/view/AuthorProfileTab.fxml"));
            Parent root = loader.load();
            AuthorProfileTabController ctrl = loader.getController();
            ctrl.bind(a, viewModel.queryService(), viewModel.background());

            Tab tab = new Tab(a.name());
            tab.setContent(root);
            tab.setOnClosed(e -> profileTabs.remove(key));
            profileTabs.put(key, tab);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
        } catch (Exception e) {
            System.err.println("Failed to open author profile tab: " + e);
            e.printStackTrace(System.err);
        }
    }

    private void openProfileTab(String key, String title, ProfileBinder binder) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gr/uoi/cs/mye030/view/ProfileTab.fxml"));
            Parent root = loader.load();
            ProfileTabController ctrl = loader.getController();
            FilterCriteria initial = viewModel.lastFilter();
            binder.apply(ctrl, initial);

            Consumer<FilterCriteria> refresher = ctrl::refresh;
            viewModel.registerProfileRefresher(refresher);

            Tab tab = new Tab(title);
            tab.setContent(root);
            tab.setOnClosed(e -> {
                viewModel.deregisterProfileRefresher(refresher);
                profileTabs.remove(key);
            });
            profileTabs.put(key, tab);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
        } catch (Exception e) {
            System.err.println("Failed to open profile tab: " + e);
            e.printStackTrace(System.err);
        }
    }

    @FunctionalInterface
    private interface ProfileBinder {
        void apply(ProfileTabController ctrl, FilterCriteria initial);
    }
}
