package pgl.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import pgl.app.model.Hospital;
import pgl.app.model.MapManager;
import pgl.app.model.VictimIncident;
import java.util.Random;

public class SidebarController {

    @FXML
    private Label infoLabel;
    private final Random random = new Random();

    private MapManager mapManager;
    private MapController mapController;

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    public void setMapController(MapController mapController) {
        this.mapController = mapController;
    }

    @FXML
    public void initialize() {
        System.out.println("SidebarController initialized.");
    }

    @FXML
    private void handleAddSite() {
        if (mapManager == null) {
            return;
        }

        int id = mapManager.getSites().size() + 1;

        double x = 80 + random.nextDouble() * 500;
        double y = 80 + random.nextDouble() * 400;

        mapManager.addHospital(new Hospital(x, y, id, 100));
        mapController.refreshMap();
        infoLabel.setText("Hospital added: " + id);
    }

    @FXML
    private void handleAddUserPoint() {
        if (mapManager == null) {
            return;
        }

        int count = mapManager.getIncidents().size() + 1;

        double x = 80 + random.nextDouble() * 500;
        double y = 80 + random.nextDouble() * 400;

        String incidentId = "INC-" + count;
        mapManager.addIncident(new VictimIncident(x, y, incidentId, "GENERAL"));
        mapController.refreshMap();
        infoLabel.setText("Incident added.");
    }

    @FXML
    private void handleRecompute() {
        if (mapManager == null) {
            return;
        }

        mapManager.updateAll();
        mapController.refreshMap();
        infoLabel.setText("Triangulation recomputed.");
    }

    @FXML
    private void handleClear() {
        if (mapManager == null) {
            return;
        }

        mapManager.clear();
        mapController.clearMap();
        infoLabel.setText("Map cleared.");
    }
}