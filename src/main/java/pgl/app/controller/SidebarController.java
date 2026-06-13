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

/**
 * Controller class for the JavaFX sidebar user interface.
 * <p>
 * This class handles UI interactions, manages map stats visualization, renders detailed descriptions
 * for selected objects (Hospitals, Incidents, Delaunay Triangles), and triggers operations such as
 * importing maps or generating simulation data.
 * </p>
 * <p>
 * It integrates Role-Based Access Control (RBAC) via {@link SecurityContext} to restrict UI buttons and
 * sensitive data access (e.g., medical data or GDPR algorithmic explanation reports) based on whether
 * the user is an Admin, Doctor, or Paramedic.
 * </p>
 *
 * @version 1.0
 */
public class SidebarController {

    // =========================================================================
    // FXML Labels (Statistics & Status)
    // =========================================================================

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

    // =========================================================================
    // FXML Text Areas & ComboBoxes
    // =========================================================================

    /** Text area used to show comprehensive, contextual data about the selected map entity. */
    @FXML
    private TextArea selectedDetailsArea;

    /** Dropdown choice box used to simulate or change the current user's security role. */
    @FXML
    private ComboBox<UserRole> roleComboBox;

    // =========================================================================
    // FXML Action Buttons
    // =========================================================================

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
    private Button addRandomHospitalsButton;

    @FXML
    private Button addRandomRoadsButton;

    @FXML
    private Button saveBinaryButton;

    @FXML
    private Button recomputeButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button deleteSelectedButton;

    // =========================================================================
    // Business Logic Fields
    // =========================================================================

    /** Random number generator for generating arbitrary geometric coordinates. */
    private final Random random = new Random();

    /** The backend data model coordinator containing infrastructure lists and topology algorithms. */
    private MapManager mapManager;

    /** The main structural map view controller tasked with handling UI rendering updates. */
    private MapController mapController;

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Refreshes the structural metadata metrics on the sidebar UI.
     * Updates counters for hospitals, registered incidents, and current Delaunay mesh triangles.
     */
    private void updateStats() {
        if (mapManager == null) {
            return;
        }

        hospitalCountLabel.setText("Hospitals: " + mapManager.getSites().size());
        incidentCountLabel.setText("Incidents: " + mapManager.getIncidents().size());
        triangleCountLabel.setText("Triangles: " + mapManager.getTriangles().size());
    }

    /**
     * Updates the status bar tracking the absolute latest incident created in the system
     * alongside its algorithmic emergency hospital destination mapping.
     */
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

    /**
     * Toggles the disabled/enabled states of the FXML interactive control items
     * depending on the active security privileges evaluated through the {@link SecurityContext}.
     */
    private void updateRoleView() {
        boolean admin = SecurityContext.hasAccess(UserRole.ADMIN);

        boolean doctorOrAdmin = SecurityContext.hasAccess(
                UserRole.ADMIN,
                UserRole.DOCTOR
        );

        addSiteButton.setDisable(!admin);
        importHospitalsCsvButton.setDisable(!admin);
        importMapButton.setDisable(!admin);
        clearButton.setDisable(!admin);
        deleteSelectedButton.setDisable(!admin);

        importIncidentsCsvButton.setDisable(!doctorOrAdmin);
        recomputeButton.setDisable(!doctorOrAdmin);
        addRandomUsersButton.setDisable(!admin);

        addRandomHospitalsButton.setDisable(!admin);
        addRandomRoadsButton.setDisable(!admin);

        importIncidentsCsvButton.setDisable(!admin);
        saveBinaryButton.setDisable(!admin);

        addUserPointButton.setDisable(!doctorOrAdmin);

        infoLabel.setText("Current role: " + SecurityContext.getCurrentRole());
    }

    // =========================================================================
    // Public API / Dependency Injection Controllers
    // =========================================================================

    /**
     * Injects the shared application {@link MapManager} dependency model data context.
     * * @param mapManager the application data layer context instance
     */
    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
        updateStats();
        updateLastAssignment();
    }

    /**
     * Injects the peer graphical rendering layout {@link MapController}.
     * * @param mapController the drawing canvas controller instance
     */
    public void setMapController(MapController mapController) {
        this.mapController = mapController;
    }

    /**
     * Evaluates and formats data regarding a selected hospital, including geospatial analysis
     * metrics such as its bounding Voronoi cell area and historical incident clustering density.
     * * @param hospital the target {@link Hospital} instance to inspect, or {@code null} to wipe details
     */
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

        // --- Geometric Calculation of Area and Density ---
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
                // Multiply by 10,000 for human-readable scales
                density = cellArea > 0 ? ((double) assignedIncidents / cellArea) * 10000 : 0.0;
            }
        }

        selectedTypeLabel.setText("Type: Hospital");
        selectedIdLabel.setText("Id: H" + hospital.getId());

        selectedDetailsArea.setText(
                "Position: (" + (int) hospital.getX() + ", " + (int) hospital.getY() + ")\n"
                        + "Capacity: " + hospital.getCapacityMax() + "\n"
                        + "Assigned incidents: " + assignedIncidents + "\n"
                        + "Specialities: " + hospital.getSpecialties() + "\n"
                        + "Min distance: " + String.format("%.2f", minDistance) + "\n"
                        + "Max distance: " + String.format("%.2f", maxDistance) + "\n"
                        + "Avg distance: " + String.format("%.2f", averageDistance) + "\n"
                        + "Cell Area: " + (cellArea > 0 ? String.format("%.2f px²", cellArea) : "Infinite/Unbounded") + "\n"
                        + "Incident Density: " + (cellArea > 0 ? String.format("%.2f inc / 10k px²", density) : "N/A")
        );
    }

    /**
     * Builds and appends metadata outputs for a highlighted incident.
     * <p>
     * <b>Security Guardrails:</b>
     * </p>
     * <ul>
     * <li>Patient medical logs are restricted solely to the {@link UserRole#DOCTOR}.</li>
     * <li>The GDPR Algorithmic Decision Report requires either an {@link UserRole#ADMIN} or {@link UserRole#DOCTOR} role.</li>
     * </ul>
     *
     * @param incident the target {@link VictimIncident} instance to inspect, or {@code null} to wipe details
     */
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

        StringBuilder details = new StringBuilder();
        details.append("Position: (").append((int) incident.getX()).append(", ").append((int) incident.getY()).append(")\n");
        details.append("Emergency type: ").append(incident.getEmergencyType()).append("\n");
        details.append("Assigned hospital: ").append(assignedHospital).append("\n");

        // --- BLOCK 1: Raw Medical History (Strictly DOCTOR access) ---
        if (SecurityContext.hasAccess(UserRole.DOCTOR)) {
            if (incident.hasMedicalHistory()) {
                details.append("Patient history: known at hospital H").append(incident.getPreferredHospitalId()).append("\n");
                details.append(incident.getMedicalNotes());
            } else {
                details.append("Patient history: none\n");
            }
        }
        details.append("\n"); // Structural spacing line

        // --- BLOCK 2: GDPR Algorithmic Transparency Explainer (DOCTOR & ADMIN access) ---
        if (incident.getClosestHospital() != null) {

            if (SecurityContext.hasAccess(UserRole.DOCTOR, UserRole.ADMIN)) {
                try {
                    // Re-instantiate routing engine processing checks to re-evaluate properties
                    DispatchEngine engine = new DispatchEngine();
                    DispatchDecision decision = engine.evaluateBestDispatch(
                            incident,
                            mapManager.getSites(),
                            mapManager.getRoutingEngine(),
                            mapManager.getTriangles()
                    );

                    GDPRReportingService reporter = new GDPRReportingService();
                    String gdprReport = reporter.generateGDPRSummary(incident, decision);

                    details.append("--- GDPR TRANSPARENCY REPORT ---\n");
                    details.append(gdprReport);

                } catch (Exception e) {
                    details.append("[Error generating GDPR report: ").append(e.getMessage()).append("]\n");
                }
            } else {
                // Low privilege warning display configuration
                details.append("--- GDPR TRANSPARENCY REPORT ---\n");
                details.append("[RESTRICTED] ADMIN or DOCTOR Role required to view algorithmic dispatch logic and medical data.");
            }
        }

        selectedDetailsArea.setText(details.toString());
        selectedDetailsArea.positionCaret(0); // Scroll view back to top
    }

    /**
     * Extracts and displays information about a clicked Delaunay Triangle.
     * Computes surface area, edge vector distances, and tracks traffic load disparities
     * across the structural network.
     * * @param triangle the targeted mesh element layout node
     */
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

    /**
     * Resets the structural details description components to their default states.
     */
    public void clearSelectionDetails() {
        selectedTypeLabel.setText("Type: none");
        selectedIdLabel.setText("Id: none");
        selectedDetailsArea.setText("Details: none");
    }

    // =========================================================================
    // JavaFX Lifecycle Initialization
    // =========================================================================

    /**
     * Automatically invoked by JavaFX loaders once FXML component mapping routines complete.
     * Attaches structural listeners onto controls and runs startup validation routines.
     */
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

    // =========================================================================
    // FXML Action Event Handlers
    // =========================================================================

    /**
     * Spawns a single hospital node at random layout coordinates.
     * Prevents operations if space overlaps are triggered.
     */
    @FXML
    private void handleAddSite() {
        if (mapManager == null) {
            return;
        }

        int id = mapManager.getSites().size() + 1;

        double x = 80 + random.nextDouble() * 500;
        double y = 80 + random.nextDouble() * 400;

        Hospital h = new Hospital(x, y, id, 100);
        h.addSpecialty(MedicalSpecialty.GENERAL);

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

    /**
     * Spawns a FileChooser dialog to parse and add structured hospital records from external CSV sheets.
     */
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

    /**
     * Clears existing state and imports map geometric topology structures from OpenStreetMap JSON
     * extensions or proprietary compiled binary formats (.pglm).
     */
    @FXML
    private void handleImportMap() {
        if (mapManager == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Map Topology");
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
            mapManager.clear();
            mapController.clearSelection();

            String fileName = selectedFile.getName();
            Path filePath = Path.of(selectedFile.getAbsolutePath());

            if (fileName.endsWith(".json")) {
                MapImporterOSM.importFromOSM(mapManager, filePath);
                infoLabel.setText("OSM Map successfully imported.");
            } else if (fileName.endsWith(".pglm")) {
                MapBinarySerializer.importFromFile(mapManager, filePath);
                infoLabel.setText("Binary Map successfully imported.");
            }

            mapController.refreshMap();
            updateStats();
            updateLastAssignment();
            clearSelectionDetails();

        } catch (Exception e) {
            infoLabel.setText("Critical error during map import.");
            e.printStackTrace();
        }
    }

    /**
     * Serializes the current hospital node infrastructures and road maps into a binary file format (.pglm).
     * * @param event the triggered FXML UI action source event
     */
    @FXML
    private void handleSaveBinary(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder l'infrastructure (.pglm)");
        fileChooser.setInitialFileName("map.pglm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers de carte PGLM (*.pglm)", "*.pglm")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                MapBinarySerializer.exportToFile(mapManager, file.toPath());
                System.out.println("Carte sauvegardée avec succès : " + file.getName());
            } catch (IOException e) {
                System.err.println("Erreur lors de l'export binaire : " + e.getMessage());
            }
        }
    }

    /**
     * Injects an incident event into a randomized location on the canvas.
     * Applies a 20% probability that the generated incident defaults to a preferred hospital setting.
     */
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

    /**
     * Parses and appends incoming incident tracking records from an external CSV data spreadsheet.
     */
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

    /**
     * Forces the data model system layers to re-evaluate Delaunay configurations,
     * proximity lookups, and shortest paths.
     */
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

    /**
     * Instructs the peer map canvas to drop or delete the selected geometric node element.
     */
    @FXML
    private void handleDeleteSelected() {
        if (mapController != null) {
            mapController.deleteSelectedElement();
            updateStats();
            updateLastAssignment();
            infoLabel.setText("Selected element deleted.");
        }
    }

    /**
     * Toggles whether Delaunay Triangulation wireframe lines are visible on the map canvas.
     */
    @FXML
    private void handleToggleTriangles() {
        if (mapController != null) {
            mapController.toggleTrianglesVisibility();
            infoLabel.setText("Affichage des triangles modifié.");
        }
    }

    /**
     * Toggles whether Voronoi cells and boundaries are visible on the map canvas.
     */
    @FXML
    private void handleToggleVoronoi() {
        if (mapController != null) {
            mapController.toggleVoronoiVisibility();
            infoLabel.setText("Affichage de Voronoï modifié.");
        }
    }

    /**
     * Erases all items from the system model context, wiping canvas nodes and statistics tracking.
     */
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

    /**
     * Generates a batch of 10 random simulated incidents across the structural tracking zones.
     */
    @FXML
    private void handleGenerateRandomIncidents() {
        int count = 10;
        int startCount = mapManager.getIncidents().size() + 1;
        List<Hospital> currentHospitals = new ArrayList<>(mapManager.getSites());

        List<VictimIncident> newIncidents = SimulationDataGenerator.generateRandomIncidents(
                count,
                startCount,
                currentHospitals
        );

        mapManager.addIncidents(newIncidents);

        if (mapController != null) {
            mapController.refreshMap();
        }
    }

    /**
     * Generates a batch of 5 random simulated hospitals across the canvas layout workspace.
     */
    @FXML
    private void handleGenerateRandomHospitals() {
        int count = 5;
        int startId = mapManager.getSites().size() + 1;

        List<Hospital> newHospitals = SimulationDataGenerator.generateRandomHospitals(count, startId);

        for (Hospital h : newHospitals) {
            try {
                mapManager.addHospital(h);
            } catch (HospitalCollisionException e) {
                System.err.println("Collision ignored in graphical mode: " + e.getMessage());
            }
        }

        if (mapController != null) {
            mapController.refreshMap();
        }
    }

    /**
     * Populates and injects a randomized network of roads connecting the registered active sites.
     */
    @FXML
    private void handleGenerateRandomRoads() {
        int nbRoads = mapManager.getSites().size() * 2;

        SimulationDataGenerator.generateRandomRoads(mapManager, nbRoads);

        if (mapController != null) {
            mapController.refreshMap();
        }
    }

    /**
     * Toggles whether incident dispatch line assignments connecting victims to hospitals are visible.
     */
    @FXML
    private void handleToggleAssignments() {
        if (mapController != null) {
            mapController.toggleAssignmentsVisibility();
            infoLabel.setText("Affichage des liaisons d'incident modifié.");
        }
    }
}