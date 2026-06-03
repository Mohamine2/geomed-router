package pgl.app.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import pgl.app.model.MapManager;

public class MainController {

    @FXML
    private AnchorPane mapContainer;

    @FXML
    private AnchorPane sidebarContainer;

    private MapController mapController;
    private SidebarController sidebarController;

    /**
     * Méthode appelée automatiquement par JavaFX après le chargement du fichier FXML.
     * C'est ici que vous initialiserez la logique de couplage entre vos sous-modules.
     */
    @FXML
    public void initialize() {
        System.out.println("MainController initialisé avec succès !");

        try {
            loadSubViews();

            // C'est ici que le Dév 4 liera le MapController et le SidebarController
            MapManager mapManager = new MapManager();
            mapController.setMapManager(mapManager);
            sidebarController.setMapManager(mapManager);
            sidebarController.setMapController(mapController);

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des sous-vues JavaFX.");
            e.printStackTrace();
        }
    }

    /**
     * Charge les vues secondaires dans leurs conteneurs respectifs.
     * map.fxml est chargé dans mapContainer
     * sidebar.fxml est chargé dans sidebarContainer
     */
    private void loadSubViews() throws IOException {
        FXMLLoader mapLoader = new FXMLLoader(getClass().getResource("/pgl/app/fxml/map.fxml"));
        AnchorPane mapView = mapLoader.load();
        mapController = mapLoader.getController();

        mapContainer.getChildren().setAll(mapView);
        AnchorPane.setTopAnchor(mapView, 0.0);
        AnchorPane.setRightAnchor(mapView, 0.0);
        AnchorPane.setBottomAnchor(mapView, 0.0);
        AnchorPane.setLeftAnchor(mapView, 0.0);

        FXMLLoader sidebarLoader = new FXMLLoader(getClass().getResource("/pgl/app/fxml/sidebar.fxml"));
        AnchorPane sidebarView = sidebarLoader.load();
        sidebarController = sidebarLoader.getController();

        sidebarContainer.getChildren().setAll(sidebarView);
        AnchorPane.setTopAnchor(sidebarView, 0.0);
        AnchorPane.setRightAnchor(sidebarView, 0.0);
        AnchorPane.setBottomAnchor(sidebarView, 0.0);
        AnchorPane.setLeftAnchor(sidebarView, 0.0);
    }
}