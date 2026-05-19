package pgl.app.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

public class MainController {

    @FXML
    private AnchorPane mapContainer;

    @FXML
    private AnchorPane sidebarContainer;

    /**
     * Méthode appelée automatiquement par JavaFX après le chargement du fichier FXML.
     * C'est ici que vous initialiserez la logique de couplage entre vos sous-modules.
     */
    @FXML
    public void initialize() {
        System.out.println("MainController initialisé avec succès !");

        // C'est ici que le Dév 4 liera le MapController et le SidebarController
    }
}