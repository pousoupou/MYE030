package gr.uoi.cs.mye030.app;

import gr.uoi.cs.mye030.repository.ArticleRepository;
import gr.uoi.cs.mye030.repository.AuthorRepository;
import gr.uoi.cs.mye030.repository.ConferenceRepository;
import gr.uoi.cs.mye030.repository.JdbcArticleRepository;
import gr.uoi.cs.mye030.repository.JdbcAuthorRepository;
import gr.uoi.cs.mye030.repository.JdbcConferenceRepository;
import gr.uoi.cs.mye030.repository.JdbcJournalRepository;
import gr.uoi.cs.mye030.repository.JournalRepository;
import gr.uoi.cs.mye030.service.QueryService;
import gr.uoi.cs.mye030.viewmodel.FilterPaneViewModel;
import gr.uoi.cs.mye030.viewmodel.MainViewModel;

public final class AppContext {

    private static AppContext instance;

    private final QueryService queryService;
    private final MainViewModel mainViewModel;
    private final FilterPaneViewModel filterPaneViewModel;

    private AppContext() {
        ArticleRepository articles = new JdbcArticleRepository();
        AuthorRepository authors = new JdbcAuthorRepository();
        JournalRepository journals = new JdbcJournalRepository();
        ConferenceRepository conferences = new JdbcConferenceRepository();

        this.queryService = new QueryService(articles, authors, journals, conferences);
        this.mainViewModel = new MainViewModel(queryService);
        this.filterPaneViewModel = new FilterPaneViewModel();
    }

    public static synchronized AppContext get() {
        if (instance == null) instance = new AppContext();
        return instance;
    }

    public QueryService queryService() { return queryService; }
    public MainViewModel mainViewModel() { return mainViewModel; }
    public FilterPaneViewModel filterPaneViewModel() { return filterPaneViewModel; }
}
