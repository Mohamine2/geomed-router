package pgl.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main entry point of the application.
 * Initializes the JavaFX environment and loads the primary user interface.
 *
 * @version 1.0
 */
public class MainApp extends Application {

    /**
     * Starts the primary stage of the JavaFX application.
     * This method is responsible for loading the main FXML layout, configuring the scene,
     * setting the window title and dimensions, and finally displaying the user interface.
     *
     * @param primaryStage The primary window (stage) for this application, onto which the application scene can be set.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pgl/app/fxml/main.fxml"));
            Parent root = loader.load();

            // Configuration de la scène principale
            Scene scene = new Scene(root);

            // TODO: Load CSS stylesheet once finalized
            // scene.getStylesheets().add(getClass().getResource("/pgl/app/css/styles.css").toExternalForm());

            primaryStage.setTitle("Optimisation Géométrique - Voronoi & Delaunay");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du fichier FXML principal.");
            e.printStackTrace();
        }
    }

    /**
     * The main entry point for the Java application.
     * It launches the JavaFX application lifecycle by calling the {@code launch} method.
     *
     * @param args The command line arguments passed to the application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}