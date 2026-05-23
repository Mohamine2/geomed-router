package pgl.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pgl.app.model.Site;
import pgl.app.model.UserPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pgl/app/fxml/main.fxml"));
            Parent root = loader.load();

            // Configuration de la scène principale
            Scene scene = new Scene(root);

            // We can load our CSS file here later
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

    public static void main(String[] args) {
        // launch(args);
        System.out.println("--- Démarrage du test MVP : Modèle ---");

        List<Site> sites = new ArrayList<>();
        sites.add(new Site(0, 0, 1));
        sites.add(new Site(10, 10, 2));
        sites.add(new Site(0, 10, 3));

        List<UserPoint> users = new ArrayList<>();
        users.add(new UserPoint(1, 1)); // Proche de (0,0) => ID: 0
        users.add(new UserPoint(9, 9)); // Proche de (10,10) => ID: 2

        // Associate each UserPoint with the closest Site
        for (UserPoint user : users) {
            Site closest = null;
            double minDistance = Double.MAX_VALUE;

            for (Site site : sites) {
                double dist = user.distanceSquaredTo(site.getX(), site.getY());
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = site;
                }
            }
            user.setClosestSite(closest);

            System.out.println("L'utilisateur aux coordonnées (" + user.getX() + "," + user.getY() +
                    ") est rattaché au Site ID: " + closest.getId());
        }

        System.out.println("--- Test terminé avec succès ---");
    }
}