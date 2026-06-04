package pgl.app;

import pgl.app.model.*;
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

    /**
     * Main entry point for the console application. Handles the main menu loop.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("--- Starting Emergency MVP Test: Console Mode ---");

        boolean running = true;
        while (running) {
            System.out.println("\nSelect an option:");
            System.out.println("1. Automated Test (Random Hospitals & Incidents)");
            System.out.println("2. Manual Test (Input Hospitals & Incidents)");
            System.out.println("3. Exit");
            System.out.print("Choice: ");

            if (sc.hasNextInt()) {
                int mode = sc.nextInt();
                switch (mode) {
                    case 1:
                        int randSites = askNaturalNumber(sc, "How many hospitals (Min 3 for triangulation)? ");
                        randomTest(sc, randSites);

                        displayAdvancedHospitalStats(mapManager.getSites());
                        displayTriangles(mapManager.getTriangles());
                        displayVoronoiCells(mapManager.getVoronoiCells());
                        displayRoutes(mapManager.getIncidents());
                        break;
                    case 2:
                        int manualSites = askNaturalNumber(sc, "How many hospitals (Min 3 for triangulation)? ");
                        manualTest(sc, manualSites);

                        displayAdvancedHospitalStats(mapManager.getSites());
                        displayTriangles(mapManager.getTriangles());
                        displayVoronoiCells(mapManager.getVoronoiCells());
                        displayRoutes(mapManager.getIncidents());
                        break;
                    case 3:
                        running = false;
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option! Please enter 1, 2, or 3.");
                }
            } else {
                System.out.println("Error: Please enter a valid number.");
                sc.next(); // Clear invalid input from the buffer
            }
        }
        sc.close();
        System.out.println("--- Test finished successfully ---");
    }

    /**
     * Performs an automated test by generating random hospitals and victim incidents.
     *
     * @param sc         The Scanner object for input.
     * @param nbHospitals The number of hospitals to generate.
     */
    public static void randomTest(Scanner sc, int nbHospitals) {
        Random rand = new Random();
        mapManager.clear();

        MedicalSpecialty[] specialties = MedicalSpecialty.values();

        System.out.println("\n--- Generating " + nbHospitals + " random hospitals ---");
        for (int i = 0; i < nbHospitals; i++) {
            // Capacity between 10 and 100 to avoid zero/negative capacity exceptions
            int capacity = rand.nextInt(91) + 10;
            Hospital h = new Hospital(rand.nextInt(201) - 100, rand.nextInt(201) - 100, i + 1, capacity);

            // Give each hospital 2 random specialties
            MedicalSpecialty randSpec = specialties[rand.nextInt(specialties.length)];
            h.addSpecialty(randSpec);
            h.addSpecialty(MedicalSpecialty.GENERAL);

            mapManager.addHospital(h);
        }

        int nbIncidents = askNaturalNumber(sc, "How many victim incidents? ");
        System.out.println("\n--- Generating " + nbIncidents + " random incidents ---");

        for (int i = 0; i < nbIncidents; i++) {
            String incidentId = "INC-R-" + String.format("%03d", i + 1);
            MedicalSpecialty emergencyType = specialties[rand.nextInt(specialties.length)];

            mapManager.addIncident(new VictimIncident(
                    rand.nextInt(201) - 100,
                    rand.nextInt(201) - 100,
                    incidentId,
                    emergencyType
            ));
        }
    }

    /**
     * Performs a manual test by prompting the user for hospital and incident parameters.
     *
     * @param sc        The Scanner object for input.
     * @param hospitalNb The number of hospitals to define.
     */
    public static void manualTest(Scanner sc, int hospitalNb) {
        mapManager.clear();

        System.out.println("\n--- Manual entry for " + hospitalNb + " hospitals ---");
        for (int i = 0; i < hospitalNb; i++) {
            System.out.println("Hospital #" + (i + 1) + ":");
            int x = askInt(sc, "  Enter X: ");
            int y = askInt(sc, "  Enter Y: ");
            int cap = askNaturalNumber(sc, "  Enter Max Capacity: ");

            Hospital h = new Hospital(x, y, i + 1, cap);
            MedicalSpecialty spec = askMedicalSpecialty(sc, "  Select primary specialty:");
            h.addSpecialty(spec);

            mapManager.addHospital(h);
        }

        int nbIncidents = askNaturalNumber(sc, "How many victim incidents? ");
        System.out.println("\n--- Manual entry for " + nbIncidents + " incidents ---");

        for (int i = 0; i < nbIncidents; i++) {
            System.out.println("Incident " + (i + 1) + ":");
            int ux = askInt(sc, "  Enter Incident X: ");
            int uy = askInt(sc, "  Enter Incident Y: ");

            MedicalSpecialty emType = askMedicalSpecialty(sc, "  Select Emergency Type:");

            String incidentId = "INC-M-" + String.format("%03d", i + 1);
            mapManager.addIncident(new VictimIncident(ux, uy, incidentId, emType));
        }
    }

    /**
     * Displays advanced operational and distance statistics for all active hospitals.
     * * @param hospitals The list of hospitals to inspect.
     */
    public static void displayAdvancedHospitalStats(List<Hospital> hospitals) {
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
     * @param triangles The list of triangles given by the DelaunayEngine.
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
     * * @param cells The list of Voronoi cells provided by MapManager.
     */
    public static void displayVoronoiCells(List<VoronoiCell> cells) {
        System.out.println("\n--- Voronoi Diagram Results (Cells) ---");
        System.out.println("Total cells generated: " + cells.size());

        if (cells.isEmpty()) {
            System.out.println("  No cells constructed (Requires at least 3 non-colinear hospitals).");
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
     * Displays the calculated optimal routes for each incident.
     * * @param incidents The list of VictimIncidents provided by MapManager.
     */
    public static void displayRoutes(List<VictimIncident> incidents) {
        System.out.println("\n--- GPS Routing Paths (Dijkstra) ---");
        for (VictimIncident incident : incidents) {
            System.out.printf("Incident %s: Target Hospital ID %d\n",
                    incident.getIncidentId(),
                    incident.getClosestSite() != null ? incident.getClosestSite().getId() : "NONE");

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
}