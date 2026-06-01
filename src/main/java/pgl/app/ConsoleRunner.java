package pgl.app;

import pgl.app.model.*;
import java.util.*;

/**
 * The ConsoleRunner class serves as the command-line interface entry point for the application.
 * It provides a robust interface to test the medical dispatch logic (Voronoi/Delaunay related models)
 * without requiring the JavaFX graphical interface.
 * * @version 2.0
 */
public class ConsoleRunner {

    /** MapManager instance to manage lists of VictimIncidents, Hospitals and Triangles */
    private static final MapManager mapManager = new MapManager();

    /** Sample pool of specialties for the automated test generator */
    private static final String[] MEDICAL_SPECIALTIES = {"CARDIOLOGY", "TRAUMATOLOGY", "NEUROLOGY", "GENERAL"};

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
                        displayStats(randomTest(sc, randSites));
                        displayTriangles(mapManager.getTriangles());
                        break;
                    case 2:
                        int manualSites = askNaturalNumber(sc, "How many hospitals (Min 3 for triangulation)? ");
                        displayStats(manualTest(sc, manualSites));
                        displayTriangles(mapManager.getTriangles());
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
     * @return A Map mapping each Hospital ID to its total number of assigned incidents.
     */
    public static Map<Integer, Integer> randomTest(Scanner sc, int nbHospitals) {
        Random rand = new Random();
        mapManager.clear();

        System.out.println("\n--- Generating " + nbHospitals + " random hospitals ---");
        for (int i = 0; i < nbHospitals; i++) {
            // Capacity between 10 and 100 to avoid zero/negative capacity exceptions
            int capacity = rand.nextInt(91) + 10;
            Hospital h = new Hospital(rand.nextInt(201) - 100, rand.nextInt(201) - 100, i + 1, capacity);

            // Give each hospital 1 to 2 random specialties
            h.addSpecialty(MEDICAL_SPECIALTIES[rand.nextInt(MEDICAL_SPECIALTIES.length)]);
            h.addSpecialty("GENERAL");

            mapManager.addHospital(h);
        }

        int nbIncidents = askNaturalNumber(sc, "How many victim incidents? ");
        System.out.println("\n--- Generating " + nbIncidents + " random incidents ---");

        for (int i = 0; i < nbIncidents; i++) {
            String incidentId = "INC-R-" + String.format("%03d", i + 1);
            String emergencyType = MEDICAL_SPECIALTIES[rand.nextInt(MEDICAL_SPECIALTIES.length)];

            mapManager.addIncident(new VictimIncident(
                    rand.nextInt(201) - 100,
                    rand.nextInt(201) - 100,
                    incidentId,
                    emergencyType
            ));
        }

        return statistics(mapManager.getIncidents(), mapManager.getSites());
    }

    /**
     * Performs a manual test by prompting the user for hospital and incident parameters.
     *
     * @param sc        The Scanner object for input.
     * @param hospitalNb The number of hospitals to define.
     * @return A Map mapping each Hospital ID to its total number of assigned incidents.
     */
    public static Map<Integer, Integer> manualTest(Scanner sc, int hospitalNb) {
        mapManager.clear();

        System.out.println("\n--- Manual entry for " + hospitalNb + " hospitals ---");
        for (int i = 0; i < hospitalNb; i++) {
            System.out.println("Hospital #" + (i + 1) + ":");
            int x = askInt(sc, "  Enter X: ");
            int y = askInt(sc, "  Enter Y: ");
            int cap = askNaturalNumber(sc, "  Enter Max Capacity: ");

            Hospital h = new Hospital(x, y, i + 1, cap);
            System.out.print("  Enter primary specialty (CARDIOLOGY, TRAUMATOLOGY, GENERAL): ");
            String spec = sc.next();
            h.addSpecialty(spec);

            mapManager.addHospital(h);
        }

        int nbIncidents = askNaturalNumber(sc, "How many victim incidents? ");
        System.out.println("\n--- Manual entry for " + nbIncidents + " incidents ---");

        for (int i = 0; i < nbIncidents; i++) {
            System.out.println("Incident " + (i + 1) + ":");
            int ux = askInt(sc, "  Enter Incident X: ");
            int uy = askInt(sc, "  Enter Incident Y: ");
            System.out.print("  Enter Emergency Type (e.g. CARDIOLOGY): ");
            String emType = sc.next();

            String incidentId = "INC-M-" + String.format("%03d", i + 1);
            mapManager.addIncident(new VictimIncident(ux, uy, incidentId, emType));
        }

        return statistics(mapManager.getIncidents(), mapManager.getSites());
    }

    /**
     * Calculates and returns a summary of incident assignments per hospital.
     *
     * @param incidents The list of victim incidents to analyze.
     * @param sites     The list of all available sites (hospitals).
     * @return A Map where the key is the Hospital ID and the value is the count of incidents.
     */
    public static Map<Integer, Integer> statistics(List<VictimIncident> incidents, List<Site> sites) {
        Map<Integer, Integer> stats = new HashMap<>();

        // Initialize map with 0 for all hospitals
        for (Site s : sites) {
            stats.put(s.getId(), 0);
        }

        // Count assignments
        for (VictimIncident idx : incidents) {
            if (idx.getClosestSite() != null) {
                int siteId = idx.getClosestSite().getId();
                stats.put(siteId, stats.getOrDefault(siteId, 0) + 1);
            }
        }

        return stats;
    }

    /**
     * Displays the statistics report in the console.
     *
     * @param stats The map containing assignments count per hospital ID.
     */
    public static void displayStats(Map<Integer, Integer> stats) {
        System.out.println("\n--- Incident Dispatch Statistics ---");
        stats.forEach((hospitalId, count) ->
                System.out.println("Hospital ID " + hospitalId + " : " + count + " active emergency case(s)"));
    }

    /**
     * Displays the triangles in the console.
     *
     * @param triangles The list of triangles given by the DelaunayEngine
     */
    public static void displayTriangles(List<Triangle> triangles) {
        System.out.println("\n--- Delaunay Triangulation Results ---");
        System.out.println("Total triangles generated: " + triangles.size());
        int i = 1;
        for (Triangle t : triangles) {
            System.out.printf("  Triangle #%d: A(%.1f, %.1f) | B(%.1f, %.1f) | C(%.1f, %.1f)\n",
                    i++,
                    t.getA().getX(), t.getA().getY(),
                    t.getB().getX(), t.getB().getY(),
                    t.getC().getX(), t.getC().getY());
        }
        System.out.println("----------------------------------------");
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