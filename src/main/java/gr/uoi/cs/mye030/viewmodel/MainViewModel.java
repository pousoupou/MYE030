package gr.uoi.cs.mye030.viewmodel;

import gr.uoi.cs.mye030.model.ArticleType;
import gr.uoi.cs.mye030.model.Author;
import gr.uoi.cs.mye030.model.Conference;
import gr.uoi.cs.mye030.model.FilterCriteria;
import gr.uoi.cs.mye030.model.Journal;
import gr.uoi.cs.mye030.service.ChartData.AuthorListRow;
import gr.uoi.cs.mye030.service.QueryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class MainViewModel {

    private final QueryService queryService;
    private final ExecutorService background = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mye030-query");
        t.setDaemon(true);
        return t;
    });

    private final ObservableList<Journal> journals = FXCollections.observableArrayList();
    private final ObservableList<Conference> conferences = FXCollections.observableArrayList();
    private final ObservableList<AuthorListRow> authors = FXCollections.observableArrayList();
    private final CopyOnWriteArrayList<Consumer<FilterCriteria>> profileRefreshers = new CopyOnWriteArrayList<>();
    private FilterCriteria lastFilter = FilterCriteria.empty();
    private IntConsumer yearProfileOpener;
    private Consumer<Author> authorProfileOpener;

    public MainViewModel(QueryService queryService) {
        this.queryService = queryService;
    }

    public ObservableList<Journal> journals() { return journals; }
    public ObservableList<Conference> conferences() { return conferences; }
    public ObservableList<AuthorListRow> authors() { return authors; }
    public QueryService queryService() { return queryService; }
    public ExecutorService background() { return background; }
    public FilterCriteria lastFilter() { return lastFilter; }

    public void registerProfileRefresher(Consumer<FilterCriteria> r) { profileRefreshers.add(r); }
    public void deregisterProfileRefresher(Consumer<FilterCriteria> r) { profileRefreshers.remove(r); }

    public void setYearProfileOpener(IntConsumer opener) { this.yearProfileOpener = opener; }
    public void openYearProfile(int year) {
        if (yearProfileOpener != null) yearProfileOpener.accept(year);
    }

    public void setAuthorProfileOpener(Consumer<Author> opener) { this.authorProfileOpener = opener; }
    public void openAuthorProfile(Author author) {
        if (author != null && authorProfileOpener != null) authorProfileOpener.accept(author);
    }

    public void refresh(FilterCriteria f) {
        this.lastFilter = f;
        ArticleType type = f == null ? null : f.type();
        if (type == ArticleType.CONFERENCE) {
            Platform.runLater(journals::clear);
        } else {
            runIntoList(journals, () -> queryService.findJournals(f));
        }
        if (type == ArticleType.JOURNAL) {
            Platform.runLater(conferences::clear);
        } else {
            runIntoList(conferences, () -> queryService.findConferences(f));
        }
        for (Consumer<FilterCriteria> r : profileRefreshers) r.accept(f);
    }

    public void loadAuthors() {
        runIntoList(authors, queryService::findAuthors);
    }

    private <T> void runIntoList(ObservableList<T> target, java.util.function.Supplier<List<T>> producer) {
        Task<List<T>> task = new Task<>() {
            @Override
            protected List<T> call() {
                return producer.get();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> target.setAll(task.getValue())));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("List query failed: " + (ex == null ? "<no exception>" : ex.toString()));
            if (ex != null) ex.printStackTrace(System.err);
        });
        background.submit(task);
    }
}
