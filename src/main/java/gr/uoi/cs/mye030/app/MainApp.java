package gr.uoi.cs.mye030.app;

import gr.uoi.cs.mye030.db.DatabaseConnectionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;

public final class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
                MainApp.class.getResource("/gr/uoi/cs/mye030/view/MainView.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                MainApp.class.getResource("/gr/uoi/cs/mye030/css/styles.css").toExternalForm());
        stage.setTitle("MYE030 — Publication Charts");
        stage.setScene(scene);
        stage.show();

        try (Connection c = DatabaseConnectionManager.getConnection()) {
            System.out.println("DB OK: " + c.getCatalog());
        } catch (Exception e) {
            System.err.println("DB probe failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
