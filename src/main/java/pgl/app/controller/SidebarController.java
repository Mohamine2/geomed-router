package pgl.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class SidebarController { @FXML private Button btnAddSite;
    @FXML private Button btnAddUser;
    @FXML private Button btnRecalculate;
    @FXML private Button btnClear;

    private MapController mapController;

    public void setMapController(MapController mapController) {
        this.mapController = mapController;
}


    @FXML
    private void handleAddSite() {
        if (mapController != null) {
            // Ajoute un site à une position aléatoire pour la démo
            double x = 50 + Math.random() * 550;
            double y = 50 + Math.random() * 500;
            mapController.drawSite(x, y, "S");
        }
    }

    @FXML
    private void handleAddUser() {
        if (mapController != null) {
            double x = 50 + Math.random() * 550;
            double y = 50 + Math.random() * 500;
            mapController.addPointOnMap(x, y);
        }
    }

    @FXML
    private void handleRecalculate() {
        System.out.println("Recalcul demandé (non branché à la logique console)");
    }

    @FXML
    private void handleClear() {
        if (mapController != null) {
            mapController.clearMap();
        }
    }
}