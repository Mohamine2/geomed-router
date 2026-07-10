package pgl.app;

import pgl.app.algo.DispatchEngine;
import pgl.app.exception.HospitalCollisionException;
import pgl.app.explainability.DispatchDecision;
import pgl.app.explainability.GDPRReportingService;
import pgl.app.io.MapBinarySerializer;
import pgl.app.model.*;
import pgl.app.security.SecurityContext;
import pgl.app.security.UserRole;
import pgl.app.test.SimulationDataGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * The ConsoleRunner class serves as the command-line interface entry point for the application.
 * It provides a robust interface to test the medical dispatch logic (Voronoi/Delaunay related models)
 * without requiring the JavaFX graphical interface.
 * * @version 3.0
 */
public class ConsoleRunner {

    /** MapManager instance to manage lists of VictimIncidents, Hospitals and Triangles */
    private static final MapManager mapManager = new MapManager();

    /** Default binary map file path used for serialization */
    private static String currentMapFile = "data/map.pglm";

    /**
     * Main entry point for the console application. Handles the main menu loop.
     *
     * @param args Command line arguments (optional file mode override).
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            currentMapFile = args[0];
            System.out.println("Default file mode : " + currentMapFile);
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("--- Urgent Emergency Dispatcher - Console Mode ---");

        boolean running = true;
        while (running) {
            System.out.println("\n===== MAIN MENU =====");
            System.out.println("1. Manage Hospitals (Add, Remove, Move)");
            System.out.println("2. Manage Incidents (Add, Remove, Move, Mass Random)");
            System.out.println("3. Mass Import Data from CSV Files");
            System.out.println("4. Inspection Panel & Specific Queries");
            System.out.println("5. Global Map Binary IO (Load/Save)");
            System.out.println("6. Full Automated Random Test Simulation");
            System.out.println("7. Import OSM Map");
            System.out.println("8. Manage Road Network (Add Roads)");
            System.out.println("9. Clear Data");
            System.out.println("10. Change User Role (RBAC)");
            System.out.println("11. Exit Application");
            System.out.print("Choice: ");

            if (sc.hasNextInt()) {
                int mode = sc.nextInt();
                switch (mode) {
                    case 1: manageHospitalsMenu(sc); break;
                    case 2: manageIncidentsMenu(sc); break;
                    case 3: importCsvMenu(sc); break;
                    case 4: inspectionMenu(sc); break;
                    case 5: binaryIoMenu(sc); break;
                    case 6:
                        int randSites = askNaturalNumber(sc, "How many hospitals (Min 3)? ");
                        randomTest(sc, randSites);
                        displayResults();
                        break;
                    case 7: importOsmMenu(sc); break;
                    case 8: manageRoadsMenu(sc); break;
                    case 9: clearAllDataMenu(); break;
                    case 10: roleChangeMenu(sc); break;
                    case 11: running = false; System.out.println("Exiting..."); break;
                    default: System.out.println("Invalid option! Please enter a number between 1 and 11.");
                }
            } else {
                System.out.println("Error: Please enter a valid number.");
                sc.next();
            }
        }
        sc.close();
    }

    /**
     * Sub-menu handling hospital lifecycle management (Add, Remove, Move).
     *
     * @param sc The Scanner object for user input.
     */
    private static void manageHospitalsMenu(Scanner sc) {
        if (!pgl.app.security.SecurityContext.hasAccess(UserRole.ADMIN)) {
            System.out.println(" Access denied : Hospital management is reserved to ADMIN Role (RBAC).");
            return;
        }
        System.out.println("\n--- Hospital Management ---");
        System.out.println("1. Add a single Hospital");
        System.out.println("2. Remove a Hospital (by ID)");
        System.out.println("3. Move an existing Hospital");
        System.out.println("0. Return to Main Menu");
        int choice = askInt(sc, "Choice: ");

        switch (choice) {
            case 0:
                return; // Retour direct à la boucle principale

            case 1: {
                int id = mapManager.getSites().size() + 1;
                double x = askInt(sc, "  Enter X: ");
                double y = askInt(sc, "  Enter Y: ");
                int cap = askNaturalNumber(sc, "  Enter Max Capacity: ");
                Hospital h = new Hospital(x, y, id, cap);
                h.addSpecialty(askMedicalSpecialty(sc, "  Select primary specialty:"));
                try {
                    mapManager.addHospital(h);
                    System.out.println("Hospital added successfully.");
                } catch (HospitalCollisionException e) {
                    System.err.println("ALERT: " + e.getMessage());
                }
                System.out.println("Hospital H" + id + " successfully added.");
                break;
            }

            case 2: {
                int id = askInt(sc, "  Enter Hospital ID to remove: ");
                Hospital h = mapManager.findHospitalById(id);
                if (h != null) {
                    mapManager.removeHospital(h);
                    System.out.println("Hospital H" + id + " removed.");
                } else {
                    System.out.println("Hospital not found.");
                }
                break;
            }

            case 3: {
                int id = askInt(sc, "  Enter Hospital ID to move: ");
                Hospital h = mapManager.findHospitalById(id);
                if (h != null) {
                    h.setX(askInt(sc, "  Enter New X: "));
                    h.setY(askInt(sc, "  Enter New Y: "));
                    mapManager.updateAll();
                    System.out.println("Hospital H" + id + " moved successfully.");
                } else {
                    System.out.println("Hospital not found.");
                }
                break;
            }

            default:
                System.out.println("Invalid option. Returning to main menu...");
                break;
        }
    }

    /**
     * Sub-menu handling victim incident lifecycle management (Add, Remove, Move, Mass Generation).
     *
     * @param sc The Scanner object for user input.
     */
    private static void manageIncidentsMenu(Scanner sc) {
        if (!pgl.app.security.SecurityContext.hasAccess(UserRole.ADMIN, UserRole.DOCTOR)) {
            System.out.println(" Access denied : Incident management is reserved to ADMIN Role (RBAC).");
            return;
        }
        System.out.println("\n--- Incident Management ---");
        System.out.println("1. Add a manual Incident");
        System.out.println("2. Remove an Incident (by ID)");
        System.out.println("3. Move an existing Incident");
        System.out.println("4. Add Mass Random Incidents (Random Positions)");
        System.out.println("0. Return to Main Menu");
        int choice = askInt(sc, "Choice: ");

        switch(choice) {
            case 0:
                return;
            case 1: {
                double ux = askInt(sc, "  Enter Incident X: ");
                double uy = askInt(sc, "  Enter Incident Y: ");
                MedicalSpecialty emType = askMedicalSpecialty(sc, "  Select Emergency Type:");

                int prefInput = askInt(sc, "  Patient has a preferred hospital ID? (Enter ID or 0 for none): ");
                Integer prefId = (prefInput > 0) ? prefInput : null;

                String id = "INC-M-" + String.format("%03d", mapManager.getIncidents().size() + 1);
                mapManager.addIncident(new VictimIncident(ux, uy, id, emType, prefId));
                System.out.println("Incident " + id + " logged and linked to closest site.");
                break;
            }

            case 2: {
                System.out.print("  Enter Incident ID to remove (ex: INC-M-001): ");
                String id = sc.next();
                VictimIncident vi = mapManager.findIncidentById(id);
                if (vi != null) {
                    mapManager.removeIncident(vi);
                    System.out.println("Incident " + id + " removed.");
                } else {
                    System.out.println("Incident not found.");
                }
                break;
            }

            case 3: {
                System.out.print("  Enter Incident ID to move: ");
                String id = sc.next();
                VictimIncident vi = mapManager.findIncidentById(id);
                if (vi != null) {
                    vi.setX(askInt(sc, "  Enter New X: "));
                    vi.setY(askInt(sc, "  Enter New Y: "));

                    vi.setClosestHospital(null);
                    mapManager.updateSingleUserAssignment(vi);

                    System.out.println("Incident " + id + " repositioned and re-assigned.");
                } else {
                    System.out.println("Incident not found.");
                }
                break;
            }

            case 4: {
                int count = askNaturalNumber(sc, "  How many random incidents to generate? ");
                int startCount = mapManager.getIncidents().size() + 1;
                List<Hospital> currentHospitals = new ArrayList<>(mapManager.getSites());

                List<VictimIncident> newIncidents = SimulationDataGenerator.generateRandomIncidents(
                        count,
                        startCount,
                        currentHospitals
                );

                mapManager.addIncidents(newIncidents);
                System.out.println(count + " random incidents generated and linked.");
                break;
            }
            default:
                System.out.println("Invalid option. Returning to main menu...");
        }
    }

    /**
     * Sub-menu providing granular structural analysis for hospitals, incidents, and geometric components.
     *
     * @param sc The Scanner object for user input.
     */
    private static void inspectionMenu(Scanner sc) {
        System.out.println("\n--- Inspection Panel ---");
        System.out.println("1. Inspect a specific Hospital & Voronoi Cell Metrics");
        System.out.println("2. Inspect a specific Victim Incident (View Zone Neighbors)");
        System.out.println("3. Inspect a specific Delaunay Triangle (Workload Disparity)");
        System.out.println("4. Display Global Dashboard (All summary stats)");
        System.out.println("0. Return to Main Menu");
        int choice = askInt(sc, "Choice: ");

        switch (choice) {
            case 0:
                return;

            case 1:
                int hId = askInt(sc, "Enter Hospital ID: ");
                Hospital h = mapManager.findHospitalById(hId);
                if (h != null) {
                    HospitalStats stats = pgl.app.algo.AnalyticsEngine.computeHospitalStats(h, mapManager.getIncidents());

                    double cellArea = 0.0;
                    if (mapManager.getVoronoiCells() != null) {
                        for (VoronoiCell cell : mapManager.getVoronoiCells()) {
                            if (cell.getHospital() != null && cell.getHospital().getId() == h.getId()) {
                                cellArea = cell.getArea();
                                break;
                            }
                        }
                    }

                    double density = pgl.app.algo.AnalyticsEngine.computeIncidentDensity(cellArea, stats.getAssignedIncidentsCount());

                    System.out.printf("\n[Hospital H%d Inspection Profile]\n", h.getId());
                    System.out.printf("  Position: (%.1f, %.1f) | Occupancy: %d/%d (%.1f%%)\n", h.getX(), h.getY(), h.getCurrentPatients(), h.getCapacityMax(), h.getOccupancyRate()*100);
                    System.out.printf("  Active Assigned Incidents workload: %d cases\n", stats.getAssignedIncidentsCount());

                    if (stats.getAssignedIncidentsCount() > 0) {
                        System.out.printf("  Response Vector Distances -> Min: %.2f | Max: %.2f | Avg: %.2f\n", stats.getMinDistance(), stats.getMaxDistance(), stats.getAverageDistance());
                    }

                    System.out.printf("  Voronoi Cell Area: %s\n", cellArea > 0 ? String.format("%.2f sq units", cellArea) : "Infinite/Unbounded");
                    System.out.printf("  Incident Density: %s\n", cellArea > 0 ? String.format("%.2f inc / 10k sq units", density) : "N/A");

                } else {
                    System.out.println("Hospital not found.");
                }
                break;

            case 2:
                System.out.print("Enter Incident ID: ");
                String viId = sc.next();
                VictimIncident vi = mapManager.findIncidentById(viId);
                if (vi != null) {
                    System.out.printf("\n[Incident %s Inspection Profile]\n", vi.getIncidentId());
                    System.out.printf("  Coordinates: (%.1f, %.1f) | Specialty Required: %s\n", vi.getX(), vi.getY(), vi.getEmergencyType());

                    if (pgl.app.security.SecurityContext.hasAccess(UserRole.DOCTOR, UserRole.ADMIN)) {
                        System.out.println("  Medical Notes : " + vi.getMedicalNotes());
                    } else {
                        System.out.println("  Medical Notes : [CONFIDENTIAL - ADMIN or DOCTOR Role required]");
                    }

                    if (vi.getClosestHospital() != null) {
                        int attachedId = vi.getClosestHospital().getId();
                        Hospital chosenHospital = (Hospital) vi.getClosestHospital();
                        System.out.println("  Linked Center: Hospital H" + attachedId);

                        // Neighbors display
                        System.out.println("  Zone Neighbors (Other victims routed to the same hospital):");
                        int countNeighbors = 0;
                        for (VictimIncident other : mapManager.getIncidents()) {
                            if (!other.getIncidentId().equals(vi.getIncidentId()) && other.getClosestHospital() != null && other.getClosestHospital().getId() == attachedId) {
                                System.out.printf("    -> %s at position (%.1f, %.1f)\n", other.getIncidentId(), other.getX(), other.getY());
                                countNeighbors++;
                            }
                        }
                        if (countNeighbors == 0) System.out.println("    No other neighbors inside this sector.");

                        if (pgl.app.security.SecurityContext.hasAccess(UserRole.DOCTOR, UserRole.ADMIN)) {
                            System.out.println("\nWould you like to generate the GDPR Transparency & Accessibility Report? (y/n)");
                            if (sc.next().equalsIgnoreCase("y")) {
                                DispatchEngine engine = new pgl.app.algo.DispatchEngine();
                                DispatchDecision decision = engine.evaluateBestDispatch(
                                        vi,
                                        mapManager.getSites(),
                                        mapManager.getRoutingEngine(),
                                        mapManager.getTriangles()
                                );

                                GDPRReportingService reporter = new GDPRReportingService();
                                String gdprReport = reporter.generateGDPRSummary(vi, decision);
                                System.out.println(gdprReport);

                                String auditLog = reporter.createAuditMessage(vi, chosenHospital);
                                System.out.println(auditLog);
                            }
                        } else {
                            System.out.println("\n[RESTRICTED] ADMIN or DOCTOR Role required.");
                        }
                    } else {
                        System.out.println("  Linked Center: None (Orphan incident)");
                    }
                } else {
                    System.out.println("Incident not found.");
                }
                break;

            case 3:
                List<Triangle> triangles = mapManager.getTriangles();
                if (triangles.isEmpty()) {
                    System.out.println("No Delaunay mesh structure computed yet.");
                    return;
                }
                System.out.println("Available Triangles index (1 to " + triangles.size() + "):");
                int tIdx = askInt(sc, "Select Index: ") - 1;

                if (tIdx >= 0 && tIdx < triangles.size()) {
                    Triangle t = triangles.get(tIdx);
                    Hospital hA = (Hospital) t.getA();
                    Hospital hB = (Hospital) t.getB();
                    Hospital hC = (Hospital) t.getC();

                    int countA = pgl.app.algo.AnalyticsEngine.getIncidentCountForHospital(hA, mapManager.getIncidents());
                    int countB = pgl.app.algo.AnalyticsEngine.getIncidentCountForHospital(hB, mapManager.getIncidents());
                    int countC = pgl.app.algo.AnalyticsEngine.getIncidentCountForHospital(hC, mapManager.getIncidents());
                    int imbalance = pgl.app.algo.AnalyticsEngine.getTriangleLoadImbalance(t, mapManager.getIncidents());

                    System.out.printf("\n[Delaunay Triangle #%d Inspection Panel]\n", (tIdx + 1));
                    System.out.printf("  Vertices coordinates: A(H%d: %.1f,%.1f) B(H%d: %.1f,%.1f) C(H%d: %.1f,%.1f)\n", hA.getId(), hA.getX(), hA.getY(), hB.getId(), hB.getX(), hB.getY(), hC.getId(), hC.getX(), hC.getY());

                    System.out.printf("  Arête metrics: AB = %.2f | BC = %.2f | CA = %.2f\n", t.getEdgeABLength(), t.getEdgeBCLength(), t.getEdgeCALength());
                    System.out.printf("  Geometric Surface Area: %.2f square units\n", t.getArea());

                    System.out.println("  Demographic Load distribution per vertex:");
                    System.out.printf("    - Hospital H%d: %d active users\n", hA.getId(), countA);
                    System.out.printf("    - Hospital H%d: %d active users\n", hB.getId(), countB);
                    System.out.printf("    - Hospital H%d: %d active users\n", hC.getId(), countC);
                    System.out.printf("  Localized Workload Imbalance Disparity: %d cases difference\n", imbalance);

                } else {
                    System.out.println("Index out of bounds.");
                }
                break;

            case 4:
                displayResults();
                break;

            default:
                System.out.println("Invalid option. Returning to main menu...");
        }
    }

    /**
     * Performs an automated test by generating random hospitals and victim incidents.
     *
     * @param sc          The Scanner object for input.
     * @param nbHospitals The number of hospitals to generate.
     */
    public static void randomTest(Scanner sc, int nbHospitals) {
        if (!SecurityContext.hasAccess(UserRole.ADMIN, UserRole.DOCTOR)) {
            System.out.println("Access Denied: Automated simulation tests are restricted to Doctors or Admins.");
            return;
        }

        List<Hospital> newHospitals = SimulationDataGenerator.generateRandomHospitals(nbHospitals, 1);

        for (Hospital h : newHospitals) {
            try {
                mapManager.addHospital(h);
            } catch (HospitalCollisionException e) {
                System.err.println("ALERT: " + e.getMessage());
            }
        }

        System.out.println(newHospitals.size() + " hospitals added successfully.");

        SimulationDataGenerator.generateRandomRoads(mapManager, nbHospitals * 2);

        int nbIncidents = askNaturalNumber(sc, "How many victim incidents? ");
        System.out.println("\n--- Generating " + nbIncidents + " random incidents ---");

        int startIncidentId = mapManager.getIncidents().size() + 1;
        List<VictimIncident> newIncidents = SimulationDataGenerator.generateRandomIncidents(
                nbIncidents, startIncidentId, new ArrayList<>(mapManager.getSites())
        );

        mapManager.addIncidents(newIncidents);
        System.out.println(newIncidents.size() + " incidents generated and linked.");
    }

    /**
     * Displays advanced operational and distance statistics for all active hospitals.
     * * @param hospitals The list of hospitals to inspect.
     */
    public static void displayAdvancedHospitalStats(Set<Hospital> hospitals) {
        System.out.println("\n--- Emergency Dispatch Advanced Statistics ---");

        for (Hospital hospital: hospitals){
            HospitalStats stats = mapManager.getStatsForHospital(hospital);

            System.out.printf("  Hospital ID %d [Capacity: %d/%d | Saturation: %.1f%%]:\n",
                    hospital.getId(),
                    hospital.getCurrentPatients(),
                    hospital.getCapacityMax(),
                    hospital.getOccupancyRate() * 100
            );

            System.out.printf("    Assigned Emergencies: %d case(s)\n", stats.getAssignedIncidentsCount());

            if (stats.getAssignedIncidentsCount() > 0){
                System.out.printf("    Intervention Vector Distances: Min = %.2f | Max = %.2f | Avg = %.2f\n",
                        stats.getMinDistance(),
                        stats.getMaxDistance(),
                        stats.getAverageDistance());
            }
            else{
                System.out.println("    Intervention Vector Distances: N/A (No active cases)");
            }
        }

        System.out.println("----------------------------------------------");
    }

    /**
     * Displays the triangles with advanced geometric and operational metrics.
     * @param triangles The list of triangles provided by the Delaunay triangulation engine.
     */
    public static void displayTriangles(List<Triangle> triangles) {
        System.out.println("\n--- Delaunay Triangulation & Inspection Results ---");
        System.out.println("Total triangles generated: " + triangles.size());
        int i = 1;
        for (Triangle t : triangles) {
            System.out.printf("  Triangle #%d: A(%.1f, %.1f) | B(%.1f, %.1f) | C(%.1f, %.1f)\n",
                    i++,
                    t.getA().getX(), t.getA().getY(),
                    t.getB().getX(), t.getB().getY(),
                    t.getC().getX(), t.getC().getY()
            );

            System.out.printf("    Edge Lengths: AB = %.2f | BC = %.2f | CA = %.2f\n",
                    t.getEdgeABLength(), t.getEdgeBCLength(), t.getEdgeCALength());
            System.out.printf("    Surface Area: %.2f square units\n", t.getArea());
            System.out.printf("    Workload Imbalance: %d active emergency case(s) difference\n",
                    mapManager.getTriangleLoadImbalance(t));
        }
        System.out.println("----------------------------------------");
    }

    /**
     * Displays the calculated Voronoi cells and their sorted vertices in the console.
     * @param cells The list of Voronoi cells provided by MapManager.
     */
    public static void displayVoronoiCells(List<VoronoiCell> cells) {
        System.out.println("\n--- Voronoi Diagram Results (Cells) ---");
        System.out.println("Total cells generated: " + cells.size());

        if (cells.isEmpty()) {
            System.out.println("Coverage areas: Too few sites to close the cells. Add more hospitals to visualize the Voronoi areas.");
        }

        for (VoronoiCell cell : cells) {
            Hospital h = cell.getHospital();
            System.out.printf("  Hospital ID %d | Location: (%.1f, %.1f) | Saturation: %.1f%% (Saturated: %b)\n",
                    h.getId(),
                    h.getX(),
                    h.getY(),
                    h.getOccupancyRate() * 100,
                    h.isSaturated());

            System.out.println("    Vertices defining the coverage zone (Sorted Polarly):");
            int vertexIndex = 1;
            for (Point vertex : cell.getVertices()) {
                System.out.printf("      Vertex #%d: X = %.2f, Y = %.2f\n",
                        vertexIndex++,
                        vertex.getX(),
                        vertex.getY());
            }
        }
        System.out.println("----------------------------------------");
    }

    /**
     * Displays the calculated optimal routes for each active incident.
     * * @param incidents The list of VictimIncidents provided by MapManager.
     */
    public static void displayRoutes(List<VictimIncident> incidents) {
        System.out.println("\n--- GPS Routing Paths (Dijkstra) ---");
        for (VictimIncident incident : incidents) {
            System.out.printf("Incident %s: Target Hospital ID ", incident.getIncidentId());

            if (incident.getClosestHospital() != null) {
                System.out.printf("%d\n", incident.getClosestHospital().getId());
            } else {
                System.out.println("NONE");
            }

            List<Point> path = mapManager.computeRoadForIncident(incident);
            if (!path.isEmpty()){
                System.out.print("  Path: ");
                path.forEach(p -> System.out.printf("(%.1f,%.1f) ", p.getX(), p.getY()));
                System.out.println();
            }
            else{
                System.out.println("No map data (bird flight)");
            }
        }
    }

    /**
     * Sub-menu to manage mass importation from a CSV file.
     *
     * @param sc the {@link Scanner} used for user input
     */
    private static void importCsvMenu(Scanner sc) {
        if (!SecurityContext.hasAccess(UserRole.ADMIN)) {
            System.out.println("Access Denied: CSV data imports are restricted to Administrators.");
            return;
        }
        System.out.println("\n--- Mass Import (CSV) ---");
        System.out.println("1. Import Hospitals (Sites)");
        System.out.println("2. Import Victim Incidents");
        System.out.println("0. Return to Main Menu");

        int choice = askInt(sc, "Choice: ");

        switch (choice) {
            case 0:
                return;

            case 1:
            case 2: {
                // On ne demande le fichier que si le choix est valide
                System.out.print("Enter the relative or absolute CSV file path (ex: test_incidents.csv): ");
                String filePath = sc.next();
                Path path = Path.of(filePath);

                if (!java.nio.file.Files.exists(path)) {
                    System.out.println("Error: File not found at " + path.toAbsolutePath());
                    return;
                }

                try {
                    if (choice == 1) {
                        int startId = mapManager.getSites().size() + 1;
                        List<Hospital> hospitals = pgl.app.io.CsvSiteImporter.importFromCsv(path, startId);

                        if (!hospitals.isEmpty()) {
                            mapManager.addHospitals(hospitals);
                            System.out.println(hospitals.size() + " hospitals successfully imported!");
                        } else {
                            System.out.println("No valid data found in the CSV.");
                        }
                    } else {
                        int startCount = mapManager.getIncidents().size() + 1;
                        List<VictimIncident> incidents = pgl.app.io.CsvIncidentImporter.importFromCsv(path, startCount);

                        if (!incidents.isEmpty()) {
                            mapManager.addIncidents(incidents);
                            System.out.println(incidents.size() + " incidents successfully imported!");
                        } else {
                            System.out.println("No valid data found in the CSV.");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error during CSV import: " + e.getMessage());
                }
                break;
            }

            default:
                System.out.println("Invalid choice.");
                break;
        }
        displayResults();
    }

    /**
     * Helper method to display comprehensive simulation metrics and topologies.
     */
    private static void displayResults() {
        displayAdvancedHospitalStats(mapManager.getSites());
        displayTriangles(mapManager.getTriangles());
        displayVoronoiCells(mapManager.getVoronoiCells());
        displayRoutes(mapManager.getIncidents());
    }

    /**
     * Sub-menu handling binary file serialization and deserialization tasks.
     *
     * @param sc The Scanner object for user input.
     */
    private static void binaryIoMenu(Scanner sc) {
        if (!pgl.app.security.SecurityContext.hasAccess(UserRole.ADMIN)) {
            System.out.println("Access Denied: Read/Write disk serialization is restricted to Administrators.");
            return;
        }
        System.out.println("\n--- Map Serialization (Binary File Format) ---");
        System.out.println("1. Load / Import entire Map Topology (" + currentMapFile + ")");
        System.out.println("2. Save / Export current Map State (" + currentMapFile + ")");
        System.out.println("0. Return to Main Menu");
        int choice = askInt(sc, "Choice: ");

        if (choice == 0) {
            return;
        } else if (choice == 1) {
            loadFromFile();
        } else if (choice == 2) {
            saveToFile();
        } else {
            System.out.println("Invalid option. Returning to main menu...");
        }
    }

    /**
     * Exports the current map structure and states into a persistent binary file.
     */
    private static void saveToFile() {
        try {
            MapBinarySerializer.exportToFile(mapManager, Path.of(currentMapFile));
            System.out.println("Map saved successfully to " + currentMapFile);
        } catch (IOException e) {
            System.err.println("Error saving map: " + e.getMessage());
        }
    }

    /**
     * Imports and reconstructs the total map infrastructure from a binary data file.
     */
    private static void loadFromFile() {
        try {
            MapBinarySerializer.importFromFile(mapManager, Path.of(currentMapFile));
            System.out.println("Map loaded successfully from " + currentMapFile);
            displayRoutes(mapManager.getIncidents());
        } catch (IOException e) {
            System.err.println("Error loading map (I/O): " + e.getMessage());
        } catch (HospitalCollisionException e) {
            System.err.println("Critical Error: The map file is corrupted. " + e.getMessage());
        }
    }

    /**
     * Displays and handles the OpenStreetMap (OSM) data import sub-menu.
     * Verifies administrative privileges, prompts the user for an OSM JSON file path,
     * validates file existence, and updates the shared map manager memory state.
     *
     * @param sc the {@link Scanner} instance used to read user input
     */
    private static void importOsmMenu(Scanner sc) {
        if (!pgl.app.security.SecurityContext.hasAccess(UserRole.ADMIN)) {
            System.out.println("Access Denied: OSM map builders are restricted to Administrators.");
            return;
        }

        System.out.println("\n--- OSM Import (JSON) ---");
        System.out.print("Enter the path to the OSM JSON file: ");
        String path = sc.next();
        Path filePath = Path.of(path);

        if (!java.nio.file.Files.exists(filePath)) {
            System.out.println("Error: File not found");
            return;
        }

        try {
            mapManager.clear();

            pgl.app.io.MapImporterOSM.importFromOSM(mapManager, filePath);

            System.out.println("Import successful! The map is now in memory.");
            System.out.println("Don't forget to click 'Save' (Option 5) to convert it to binary .pglm.");

        } catch (Exception e) {
            System.err.println("Critical import error : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sub-menu handling manual road network creation and traffic injection.
     * Restricts operations to administrative users and guides them through node positioning
     * and traffic factor adjustments.
     *
     * @param sc the {@link Scanner} instance used to read user input
     */
    private static void manageRoadsMenu(Scanner sc) {
        if (!pgl.app.security.SecurityContext.hasAccess(UserRole.ADMIN)) {
            System.out.println(" Access denied : Road network management is reserved to ADMIN Role (RBAC).");
            return;
        }

        System.out.println("\n--- Road Network Management ---");
        System.out.println("1. Add a manual Road (Custom Traffic)");
        System.out.println("0. Return to Main Menu");
        int choice = askInt(sc, "Choice: ");

        if (choice == 1) {
            System.out.println("  [Tip: Nodes will be automatically created if they don't exist]");
            double sx = askDouble(sc, "  Enter Start Node X: ");
            double sy = askDouble(sc, "  Enter Start Node Y: ");
            double ex = askDouble(sc, "  Enter End Node X: ");
            double ey = askDouble(sc, "  Enter End Node Y: ");
            double traffic = askDouble(sc, "  Enter Traffic Factor (1.0 = clear, >1.0 = heavy traffic): ");

            traffic = Math.max(1.0, traffic);

            // Route creation via MapManager (which will handle adding it to RoadNetwork and invalidating the cache)
            mapManager.addRoad(new Point(sx, sy), new Point(ex, ey), traffic);

            System.out.println("Road added successfully between (" + sx + "," + sy + ") and (" + ex + "," + ey + ") with traffic factor " + traffic);
        }
        else if(choice == 0){
            return;
        }
        else{
            System.out.println("Invalid option. Returning to main menu...");
        }
    }

    /**
     * Displays the current active user role and handles updating the execution context's role security token.
     * Provides a localized mapping between interface selections and specific authorization roles.
     *
     * @param sc the {@link Scanner} instance used to read user input
     */
    private static void roleChangeMenu(Scanner sc){
        System.out.println("\n--- Current Role: " + SecurityContext.getCurrentRole() + " ---");
        System.out.println("Select new Role:\n1. PARAMEDIC\n2. DOCTOR\n3. ADMIN");
        int roleChoice = askInt(sc, "Choice: ");
        if (roleChoice == 1) SecurityContext.setCurrentRole(UserRole.PARAMEDIC);
        else if (roleChoice == 2) SecurityContext.setCurrentRole(UserRole.DOCTOR);
        else if (roleChoice == 3) SecurityContext.setCurrentRole(UserRole.ADMIN);
        System.out.println("Role updated to: " + SecurityContext.getCurrentRole());
    }

    /**
     * Triggers a total wipe of all spatial data structures managed by the core system.
     * Access is restricted entirely to users mapped with the administrator role.
     */
    private static void clearAllDataMenu() {
        if (!SecurityContext.hasAccess(UserRole.ADMIN)) {
            System.out.println("Access Denied: Only an Administrator can clear the map data structures.");
            return;
        }
        mapManager.clear();
        System.out.println("Data cleared successfully!");
    }

    /**
     * Displays the available medical specialties as a numbered menu and robustly
     * forces the user to select a valid option.
     * <p>
     * This method dynamically reads all values from {@link MedicalSpecialty},
     * lists them to the console, and loops until the user provides a valid index
     * within the boundaries of the enum.
     * </p>
     *
     * @param sc      the {@link Scanner} object used to capture user input.
     * @param message the contextual prompt message to display before the menu.
     * @return the selected {@link MedicalSpecialty} enum constant.
     */
    private static MedicalSpecialty askMedicalSpecialty(Scanner sc, String message) {
        System.out.println(message);

        MedicalSpecialty[] specialties = MedicalSpecialty.values();

        for (int i = 0; i < specialties.length; i++) {
            System.out.printf("  %d. %s\n", (i + 1), specialties[i].name());
        }

        while (true) {
            int choice = askNaturalNumber(sc, "Your choice(number) : ");
            if (choice >= 1 && choice <= specialties.length) {
                return specialties[choice - 1];
            }
            System.out.println("Error : Number out of limits. Please retry.");
        }
    }

    /**
     * Helper method to robustly ask for a natural number.
     *
     * @param sc  The Scanner object for user input.
     * @param msg The contextual prompt message.
     * @return A valid integer strictly greater than 0.
     */
    public static int askNaturalNumber(Scanner sc, String msg) {
        while (true) {
            int n = askInt(sc, msg);
            if (n > 0) return n;
            System.out.println("Error: Please enter a number strictly greater than 0.");
        }
    }

    /**
     * Helper method to robustly request an integer from the user.
     *
     * @param sc      The Scanner object for user input.
     * @param message The contextual prompt message.
     * @return A valid integer parsed from the console.
     */
    private static int askInt(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            if (sc.hasNextInt()) {
                return sc.nextInt();
            } else {
                System.out.println("Error: Please enter a valid integer.");
                sc.next(); // Clear the invalid input
            }
        }
    }

    /**
     * Helper method to robustly request a decimal (double) from the user.
     *
     * @param sc      The Scanner object for user input.
     * @param message The contextual prompt message.
     * @return A valid double parsed from the console.
     */
    private static double askDouble(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            if (sc.hasNextDouble()) {
                return sc.nextDouble();
            } else {
                System.out.println("Error: Please enter a valid decimal number.");
                sc.next(); // Clear the invalid input
            }
        }
    }
}