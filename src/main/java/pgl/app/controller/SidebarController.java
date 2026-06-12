package pgl.app.controller;

import java.util.ArrayList;
import java.util.Random;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import pgl.app.algo.exception.HospitalCollisionException;
import pgl.app.model.*;
import javafx.scene.control.TextArea;
import java.io.File;
import java.nio.file.Path;
import javafx.stage.FileChooser;
import pgl.app.io.CsvSiteImporter;
import pgl.app.io.CsvIncidentImporter;
import java.util.List;
import pgl.app.io.MapImporterOSM;
import pgl.app.io.MapBinarySerializer;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import pgl.app.security.SecurityContext;
import pgl.app.security.UserRole;
import pgl.app.algo.DispatchEngine;
import pgl.app.explainability.DispatchDecision;
import pgl.app.explainability.GDPRReportingService;
import pgl.app.test.SimulationDataGenerator;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import java.io.IOException;


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
    
    @FXML
    private ComboBox<UserRole> roleComboBox;

    @FXML
    private Button addSiteButton;

    @FXML
    private Button importHospitalsCsvButton;

    @FXML
    private Button importMapButton;

    @FXML
    private Button addUserPointButton;

    @FXML
    private Button addRandomUsersButton;

    @FXML
    private Button importIncidentsCsvButton;

    @FXML
    private Button recomputeButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button deleteSelectedButton;

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
    
    private void updateRoleView() {
        boolean admin = SecurityContext.hasAccess(UserRole.ADMIN);

        boolean doctorOrAdmin = SecurityContext.hasAccess(
                UserRole.ADMIN,
                UserRole.DOCTOR
        );

        boolean paramedicOrAbove = SecurityContext.hasAccess(
                UserRole.ADMIN,
                UserRole.DOCTOR,
                UserRole.PARAMEDIC
        );

        addSiteButton.setDisable(!admin);
        importHospitalsCsvButton.setDisable(!admin);
        importMapButton.setDisable(!admin);
        clearButton.setDisable(!admin);
        deleteSelectedButton.setDisable(!admin);

        importIncidentsCsvButton.setDisable(!doctorOrAdmin);
        recomputeButton.setDisable(!doctorOrAdmin);
        addRandomUsersButton.setDisable(!doctorOrAdmin);

        addUserPointButton.setDisable(!paramedicOrAbove);

        infoLabel.setText("Current role: " + SecurityContext.getCurrentRole());
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

        // --- AJOUT : Calcul géométrique de la surface et de la densité ---
        double cellArea = 0.0;
        double density = 0.0;
        VoronoiCell targetCell = null;

        if (mapManager.getVoronoiCells() != null) {
            for (VoronoiCell cell : mapManager.getVoronoiCells()) {
                if (cell.getHospital() != null && cell.getHospital().getId() == hospital.getId()) {
                    targetCell = cell;
                    break;
                }
            }
        }

        if (targetCell != null && targetCell.getVertices() != null) {
            List<Point> vertices = targetCell.getVertices();
            int n = vertices.size();
            if (n >= 3) {
                double areaSum = 0.0;
                for (int i = 0; i < n; i++) {
                    Point current = vertices.get(i);
                    Point next = vertices.get((i + 1) % n);
                    areaSum += current.getX() * next.getY() - next.getX() * current.getY();
                }
                cellArea = Math.abs(areaSum) / 2.0;
                // On multiplie par 10 000 pour avoir une métrique humainement lisible
                density = cellArea > 0 ? ((double) assignedIncidents / cellArea) * 10000 : 0.0;
            }
        }

        selectedTypeLabel.setText("Type: Hospital");
        selectedIdLabel.setText("Id: H" + hospital.getId());

        selectedDetailsArea.setText(
                "Position: (" + (int) hospital.getX() + ", " + (int) hospital.getY() + ")\n"
                        + "Capacity: " + hospital.getCapacityMax() + "\n"
                        + "Assigned incidents: " + assignedIncidents + "\n"
                        + "Min distance: " + String.format("%.2f", minDistance) + "\n"
                        + "Max distance: " + String.format("%.2f", maxDistance) + "\n"
                        + "Avg distance: " + String.format("%.2f", averageDistance) + "\n"
                        + "Cell Area: " + (cellArea > 0 ? String.format("%.2f px²", cellArea) : "Infinite/Unbounded") + "\n"
                        + "Incident Density: " + (cellArea > 0 ? String.format("%.2f inc / 10k px²", density) : "N/A")
        );
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

        // On utilise un StringBuilder pour construire le texte complet
        StringBuilder details = new StringBuilder();
        details.append("Position: (").append((int) incident.getX()).append(", ").append((int) incident.getY()).append(")\n");
        details.append("Emergency type: ").append(incident.getEmergencyType()).append("\n");
        details.append("Assigned hospital: ").append(assignedHospital).append("\n");

        // --- BLOC 1 : Historique médical brut (Réservé strictement au MEDECIN) ---
        /*
         * L'historique médical est une donnée ultra-sensible.
         * L'administrateur et l'ambulancier ne voient pas cette ligne.
         */
        if (SecurityContext.hasAccess(UserRole.DOCTOR)) {
            if (incident.hasMedicalHistory()) {
                details.append("Patient history: known at hospital H").append(incident.getPreferredHospitalId()).append("\n");
                details.append(incident.getMedicalNotes());
            } else {
                details.append("Patient history: none\n");
            }
        }
        details.append("\n"); // Ligne vide pour aérer l'affichage

        // --- BLOC 2 : Intégration du rapport d'explicabilité RGPD (MEDECIN & ADMIN) ---
        if (incident.getClosestHospital() != null) {

            if (SecurityContext.hasAccess(UserRole.DOCTOR, UserRole.ADMIN)) {
                try {
                    // 1. On demande au moteur de recréer l'évaluation complète
                    DispatchEngine engine = new DispatchEngine();
                    DispatchDecision decision = engine.evaluateBestDispatch(
                            incident,
                            mapManager.getSites(),
                            mapManager.getRoutingEngine(),
                            mapManager.getTriangles()
                    );

                    // 2. On confie la décision au service de reporting
                    GDPRReportingService reporter = new GDPRReportingService();
                    String gdprReport = reporter.generateGDPRSummary(incident, decision);

                    details.append("--- GDPR TRANSPARENCY REPORT ---\n");
                    details.append(gdprReport);

                } catch (Exception e) {
                    details.append("[Error generating GDPR report: ").append(e.getMessage()).append("]\n");
                }
            } else {
                // Rôle Ambulancier (ou autre) sans accès
                details.append("--- GDPR TRANSPARENCY REPORT ---\n");
                details.append("[RESTRICTED] ADMIN or DOCTOR Role required to view algorithmic dispatch logic and medical data.");
            }
        }

        // On affiche tout le texte dans la zone de droite
        selectedDetailsArea.setText(details.toString());

        // Petite astuce JavaFX pour s'assurer que le curseur remonte en haut du texte long
        selectedDetailsArea.positionCaret(0);
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

        roleComboBox.setItems(FXCollections.observableArrayList(UserRole.values()));
        roleComboBox.getSelectionModel().select(SecurityContext.getCurrentRole());

        roleComboBox.setOnAction(event -> {
            UserRole selectedRole = roleComboBox.getSelectionModel().getSelectedItem();
            SecurityContext.setCurrentRole(selectedRole);
            updateRoleView();
        });

        updateStats();
        updateRoleView();
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
    private void handleImportHospitalsCsv() {
        if (mapManager == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Hospitals CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile == null) {
            return;
        }

        try {
            int startId = mapManager.getSites().size() + 1;

            List<Hospital> importedHospitals = CsvSiteImporter.importFromCsv(
                    Path.of(selectedFile.getAbsolutePath()),
                    startId
            );

            for (Hospital hospital : importedHospitals) {
                mapManager.addHospital(hospital);
            }

            mapController.refreshMap();
            updateStats();
            updateLastAssignment();
            infoLabel.setText(importedHospitals.size() + " hospitals imported.");

        } catch (Exception e) {
            infoLabel.setText("Import failed.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleImportMap() {
        if (mapManager == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Map Topology");
        // On autorise à la fois le JSON (OSM) et le PGLM (Binaire)
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Map Files", "*.json", "*.pglm"),
                new FileChooser.ExtensionFilter("OSM JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("Binary Map Files", "*.pglm")
        );

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile == null) {
            return;
        }

        try {
            // 1. On nettoie la carte actuelle pour éviter de fusionner deux topologies
            mapManager.clear();
            mapController.clearSelection();

            String fileName = selectedFile.getName();
            Path filePath = Path.of(selectedFile.getAbsolutePath());

            // 2. On délègue au bon importateur selon l'extension
            if (fileName.endsWith(".json")) {
                MapImporterOSM.importFromOSM(mapManager, filePath);
                infoLabel.setText("OSM Map successfully imported.");
            } else if (fileName.endsWith(".pglm")) {
                MapBinarySerializer.importFromFile(mapManager, filePath);
                infoLabel.setText("Binary Map successfully imported.");
            }

            // 3. On met à jour l'interface graphique
            mapController.refreshMap();
            updateStats();
            updateLastAssignment();
            clearSelectionDetails();

        } catch (Exception e) {
            infoLabel.setText("Critical error during map import.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveBinary(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder l'infrastructure (.pglm)");
        fileChooser.setInitialFileName("map.pglm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers de carte PGLM (*.pglm)", "*.pglm")
        );

        // Récupération de la fenêtre parente pour centrer la boîte de dialogue
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                // Utilisation exacte de votre sérialiseur binaire issu de ConsoleRunner
                MapBinarySerializer.exportToFile(mapManager, file.toPath());
                System.out.println("Carte sauvegardée avec succès : " + file.getName());
            } catch (IOException e) {
                System.err.println("Erreur lors de l'export binaire : " + e.getMessage());
                // Optionnel : Ajouter une boîte de dialogue contextuelle Alert pour notifier l'échec
            }
        }
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

        Integer prefId = null;
        List<Hospital> currentHospitals = new ArrayList<>(mapManager.getSites());
        if (!currentHospitals.isEmpty() && random.nextDouble() < 0.20) {
            Hospital randomHosp = currentHospitals.get(random.nextInt(currentHospitals.size()));
            prefId = randomHosp.getId();
        }

        mapManager.addIncident(new VictimIncident(x, y, incidentId, MedicalSpecialty.GENERAL, prefId));

        mapController.refreshMap();

        if (prefId != null) {
            infoLabel.setText("Incident added (Prefers H" + prefId + ").");
        } else {
            infoLabel.setText("Incident added.");
        }

        updateStats();
        updateLastAssignment();
    }
    
    @FXML
    private void handleImportIncidentsCsv() {
        if (mapManager == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Incidents CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile == null) {
            return;
        }

        try {
            int startCount = mapManager.getIncidents().size() + 1;

            List<VictimIncident> importedIncidents = CsvIncidentImporter.importFromCsv(
                    Path.of(selectedFile.getAbsolutePath()),
                    startCount
            );

            for (VictimIncident incident : importedIncidents) {
                mapManager.addIncident(incident);
            }

            mapController.refreshMap();
            updateStats();
            updateLastAssignment();
            infoLabel.setText(importedIncidents.size() + " incidents imported.");

        } catch (Exception e) {
            infoLabel.setText("Incident import failed.");
            e.printStackTrace();
        }
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
    private void handleDeleteSelected() {
        if (mapController != null) {
            mapController.deleteSelectedElement();
            updateStats();
            updateLastAssignment();
            infoLabel.setText("Selected element deleted.");
        }
    }

    @FXML
    private void handleToggleTriangles() {
        if (mapController != null) {
            mapController.toggleTrianglesVisibility();
            infoLabel.setText("Affichage des triangles modifié.");
        }
    }

    @FXML
    private void handleToggleVoronoi() {
        if (mapController != null) {
            mapController.toggleVoronoiVisibility();
            infoLabel.setText("Affichage de Voronoï modifié.");
        }
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

    @FXML
    private void handleGenerateRandomIncidents() {
        // Exemple : 10 incidents (ou Integer.parseInt(txtIncidentCount.getText()) si vous avez un champ)
        int count = 10;
        int startCount = mapManager.getIncidents().size() + 1;
        List<Hospital> currentHospitals = new ArrayList<>(mapManager.getSites());

        // 1. Génération via la classe utilitaire commune
        List<VictimIncident> newIncidents = SimulationDataGenerator.generateRandomIncidents(
                count,
                startCount,
                currentHospitals
        );

        // 2. Ajout en masse dans le modèle
        mapManager.addIncidents(newIncidents);

        // 3. Rafraîchissement de la vue JavaFX
        if (mapController != null) {
            mapController.refreshMap();
        }
    }


    @FXML
    private void handleGenerateRandomHospitals() {
        // Exemple : génération de 5 hôpitaux (vous pouvez aussi récupérer cette valeur depuis un TextField)
        int count = 5;
        int startId = mapManager.getSites().size() + 1;

        // 1. Génération via la classe utilitaire commune
        List<Hospital> newHospitals = SimulationDataGenerator.generateRandomHospitals(count, startId);

        // 2. Ajout au gestionnaire avec gestion des collisions
        for (Hospital h : newHospitals) {
            try {
                mapManager.addHospital(h);
            } catch (HospitalCollisionException e) {
                // En JavaFX, on privilégie un log ou une alerte discrète plutôt qu'un plantage
                System.err.println("Collision ignorée en mode graphique : " + e.getMessage());
            }
        }

        // 3. Rafraîchissement crucial de la vue JavaFX
        if (mapController != null) {
            mapController.refreshMap();
        }
    }

    @FXML
    private void handleGenerateRandomRoads() {
        int nbRoads = mapManager.getSites().size() * 2;

        // 1. Génération et injection directe dans le RoadNetwork du MapManager
        SimulationDataGenerator.generateRandomRoads(mapManager, nbRoads);

        // 2. Rafraîchissement de la vue JavaFX (pour tracer les lignes de trafic gris/orange/rouge)
        if (mapController != null) {
            mapController.refreshMap();
        }
    }

    @FXML
    private void handleToggleAssignments() {
        if (mapController != null) {
            mapController.toggleAssignmentsVisibility();
            infoLabel.setText("Affichage des liaisons d'incident modifié.");
        }
    }
}