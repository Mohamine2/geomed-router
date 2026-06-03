package pgl.app.controller;

import java.util.Random;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import pgl.app.model.Hospital;
import pgl.app.model.MapManager;
import pgl.app.model.VictimIncident;
import pgl.app.model.MedicalSpecialty;

public class SidebarController {

    @FXML
    private Label infoLabel;
    
    @FXML
    private Label hospitalCountLabel;

    @FXML
    private Label incidentCountLabel;

    @FXML
    private Label triangleCountLabel;
    
    @FXML
    private Label lastIncidentLabel;

    @FXML
    private Label assignedHospitalLabel;

    private final Random random = new Random();

    private MapManager mapManager;
    private MapController mapController;
    
    private void updateStats() {
        if (mapManager == null) {
            return;
        }

        hospitalCountLabel.setText("Hospitals: " + mapManager.getSites().size());
        incidentCountLabel.setText("Incidents: " + mapManager.getIncidents().size());
        triangleCountLabel.setText("Triangles: " + mapManager.getTriangles().size());
    }
    
    private void updateLastAssignment() {
        if (mapManager == null || mapManager.getIncidents().isEmpty()) {
            lastIncidentLabel.setText("Last incident: none");
            assignedHospitalLabel.setText("Assigned hospital: none");
            return;
        }

        VictimIncident lastIncident = mapManager.getIncidents()
                .get(mapManager.getIncidents().size() - 1);

        lastIncidentLabel.setText("Last incident: " + lastIncident.getIncidentId());

        if (lastIncident.getClosestSite() != null) {
            assignedHospitalLabel.setText("Assigned hospital: H" + lastIncident.getClosestSite().getId());
        } else {
            assignedHospitalLabel.setText("Assigned hospital: none");
        }
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
        updateStats();
        updateLastAssignment();
    }

    public void setMapController(MapController mapController) {
        this.mapController = mapController;
    }

    @FXML
    public void initialize() {
        System.out.println("SidebarController initialized.");
        updateStats();
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
        
        updateStats();
        updateLastAssignment();
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
        mapManager.addIncident(new VictimIncident(x, y, incidentId, MedicalSpecialty.GENERAL));
        mapController.refreshMap();
        infoLabel.setText("Incident added.");
        
        updateStats();
        updateLastAssignment();
    }

    @FXML
    private void handleRecompute() {
        if (mapManager == null) {
            return;
        }

        mapManager.updateAll();
        mapController.refreshMap();
        infoLabel.setText("Triangulation recomputed.");
        
        updateStats();
        updateLastAssignment();
    }

    @FXML
    private void handleClear() {
        if (mapManager == null) {
            return;
        }

        mapManager.clear();
        mapController.clearMap();
        infoLabel.setText("Map cleared.");
        
        updateStats();
        updateLastAssignment();
    }
}