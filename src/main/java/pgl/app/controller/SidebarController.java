package pgl.app.controller;

import java.util.Random;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import pgl.app.algo.exception.HospitalCollisionException;
import pgl.app.model.Hospital;
import pgl.app.model.MapManager;
import pgl.app.model.VictimIncident;
import pgl.app.model.MedicalSpecialty;
import pgl.app.model.Triangle;
import javafx.scene.control.TextArea;


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
    
    @FXML
    private Label selectedTypeLabel;

    @FXML
    private Label selectedIdLabel;

    @FXML
    private TextArea selectedDetailsArea;

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

        if (lastIncident.getClosestHospital() != null) {
            assignedHospitalLabel.setText("Assigned hospital: H" + lastIncident.getClosestHospital().getId());
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
    
    public void showHospitalDetails(Hospital hospital) {
        if (hospital == null) {
            selectedTypeLabel.setText("Type: none");
            selectedIdLabel.setText("Id: none");
            selectedDetailsArea.setText("Details: none");
            return;
        }

        int assignedIncidents = 0;
        double minDistance = Double.MAX_VALUE;
        double maxDistance = 0.0;
        double totalDistance = 0.0;

        for (VictimIncident incident : mapManager.getIncidents()) {
            if (incident.getClosestHospital() != null
                    && incident.getClosestHospital().getId() == hospital.getId()) {

                assignedIncidents++;

                double dx = incident.getX() - hospital.getX();
                double dy = incident.getY() - hospital.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < minDistance) {
                    minDistance = distance;
                }
                if (distance > maxDistance) {
                    maxDistance = distance;
                }

                totalDistance += distance;
            }
        }

        if (assignedIncidents == 0) {
            minDistance = 0.0;
        }

        double averageDistance = assignedIncidents > 0 ? totalDistance / assignedIncidents : 0.0;

        selectedTypeLabel.setText("Type: Hospital");
        selectedIdLabel.setText("Id: H" + hospital.getId());
        selectedDetailsArea.setText(
                "Position: (" + (int) hospital.getX() + ", " + (int) hospital.getY() + ")\n" +
                "Capacity: " + hospital.getCapacityMax() + "\n" +
                "Assigned incidents: " + assignedIncidents + "\n" +
                "Min distance: " + String.format("%.2f", minDistance) + "\n" +
                "Max distance: " + String.format("%.2f", maxDistance) + "\n" +
                "Avg distance: " + String.format("%.2f", averageDistance)
        );
        System.out.println(selectedDetailsArea.getText());
    }
    
    public void showIncidentDetails(VictimIncident incident) {
        if (incident == null) {
            selectedTypeLabel.setText("Type: none");
            selectedIdLabel.setText("Id: none");
            selectedDetailsArea.setText("Details: none");
            return;
        }

        selectedTypeLabel.setText("Type: Incident");
        selectedIdLabel.setText("Id: " + incident.getIncidentId());

        String assignedHospital = "none";
        if (incident.getClosestHospital() != null) {
            assignedHospital = "H" + incident.getClosestHospital().getId();
        }

        selectedDetailsArea.setText(
                "Position: (" + (int) incident.getX() + ", " + (int) incident.getY() + ")\n" +
                "Emergency type: " + incident.getEmergencyType() + "\n" +
                "Assigned hospital: " + assignedHospital
        );
    }
    
    public void showTriangleDetails(Triangle triangle) {
        if (triangle == null) {
            selectedTypeLabel.setText("Type: none");
            selectedIdLabel.setText("Id: none");
            selectedDetailsArea.setText("Details: none");
            return;
        }

        Hospital h1 = (Hospital) triangle.getA();
        Hospital h2 = (Hospital) triangle.getB();
        Hospital h3 = (Hospital) triangle.getC();

        int countH1 = 0;
        int countH2 = 0;
        int countH3 = 0;

        for (VictimIncident incident : mapManager.getIncidents()) {
            if (incident.getClosestHospital() != null) {
                int siteId = incident.getClosestHospital().getId();

                if (siteId == h1.getId()) countH1++;
                if (siteId == h2.getId()) countH2++;
                if (siteId == h3.getId()) countH3++;
            }
        }

        double area = Math.abs(
                h1.getX() * (h2.getY() - h3.getY()) +
                h2.getX() * (h3.getY() - h1.getY()) +
                h3.getX() * (h1.getY() - h2.getY())
        ) / 2.0;

        double edge12 = Math.sqrt(Math.pow(h1.getX() - h2.getX(), 2) + Math.pow(h1.getY() - h2.getY(), 2));
        double edge23 = Math.sqrt(Math.pow(h2.getX() - h3.getX(), 2) + Math.pow(h2.getY() - h3.getY(), 2));
        double edge31 = Math.sqrt(Math.pow(h3.getX() - h1.getX(), 2) + Math.pow(h3.getY() - h1.getY(), 2));

        int maxLoad = Math.max(countH1, Math.max(countH2, countH3));
        int minLoad = Math.min(countH1, Math.min(countH2, countH3));
        int workloadImbalance = maxLoad - minLoad;

        selectedTypeLabel.setText("Type: Triangle");
        selectedIdLabel.setText("Id: H" + h1.getId() + "-H" + h2.getId() + "-H" + h3.getId());
        selectedDetailsArea.setText(
                "Vertices: H" + h1.getId() + ", H" + h2.getId() + ", H" + h3.getId() + "\n" +
                "Area: " + String.format("%.2f", area) + "\n" +
                "Edge H" + h1.getId() + "-H" + h2.getId() + ": " + String.format("%.2f", edge12) + "\n" +
                "Edge H" + h2.getId() + "-H" + h3.getId() + ": " + String.format("%.2f", edge23) + "\n" +
                "Edge H" + h3.getId() + "-H" + h1.getId() + ": " + String.format("%.2f", edge31) + "\n" +
                "Assigned incidents:\n" +
                "H" + h1.getId() + ": " + countH1 + "\n" +
                "H" + h2.getId() + ": " + countH2 + "\n" +
                "H" + h3.getId() + ": " + countH3 + "\n" +
                "Workload imbalance: " + workloadImbalance
        );
    }
    
    public void clearSelectionDetails() {
        selectedTypeLabel.setText("Type: none");
        selectedIdLabel.setText("Id: none");
        selectedDetailsArea.setText("Details: none");
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

        Hospital h = new Hospital(x, y, id, 100);
        h.addSpecialty(MedicalSpecialty.GENERAL); // Optionnel: donne une spécialité par défaut

        try {
            mapManager.addHospital(h);

            mapController.refreshMap();
            infoLabel.setText("Hospital added: " + id);

            updateStats();
            updateLastAssignment();

        } catch (HospitalCollisionException e) {
            infoLabel.setText("Error: Intersection already occupied!");
            System.err.println("UI Blocked: " + e.getMessage());
        }
        
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
        mapController.clearSelection();
        mapController.clearMap();
        infoLabel.setText("Map cleared.");
        
        updateStats();
        updateLastAssignment();
        clearSelectionDetails();
    }
}