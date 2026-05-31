package pgl.app;

import pgl.app.model.*;
import java.util.*;

/**
 * The ConsoleRunner class serves as the command-line interface entry point for the application.
 * It provides a robust interface to test the logic (Voronoi/Delaunay related models)
 * without requiring the JavaFX graphical interface.
 * * @version 1.0
 */
public class ConsoleRunner {

    /** MapManager instance to manage lists of UserPoints, Sites and Triangles  */
    private static final MapManager mapManager = new MapManager();

    /**
     * Main entry point for the console application. Handles the main menu loop.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("--- Starting MVP Test: Console Mode ---");

        boolean running = true;
        while (running) {
            System.out.println("\nSelect an option:");
            System.out.println("1. Automated Test (Random Sites & Users)");
            System.out.println("2. Manual Test (Input Sites & Users)");
            System.out.println("3. Exit");
            System.out.print("Choice: ");

            if (sc.hasNextInt()) {
                int mode = sc.nextInt();
                switch (mode) {
                    case 1:
                        displayStats(randomTest(sc, askNaturalNumber(sc, "How many sites (You need at least 3 sites to create a triangle) ? ")));
                        displayTriangles(mapManager.getTriangles());
                        break;
                    case 2:
                        displayStats(manualTest(sc, askNaturalNumber(sc, "How many sites (You need at least 3 sites to create a triangle) ? ")));
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
                sc.next();
            }
        }
        sc.close();
        System.out.println("--- Test finished successfully ---");
    }

    /**
     * Performs an automated test by generating random sites and random user points.
     *
     * @param sc       The Scanner object for input.
     * @param nbSites  The number of sites to generate.
     * @return A Map mapping each Site ID to its total number of assigned users.
     */
    public static Map<Integer, Integer> randomTest(Scanner sc, int nbSites) {
        Random rand = new Random();

        mapManager.clear();

        System.out.println("\n--- Generating " + nbSites + " random sites ---");
        for (int i = 0; i < nbSites; i++) {
            mapManager.addSite(new Site(rand.nextInt(201) - 100, rand.nextInt(201) - 100, i + 1));
        }

        int nbUsers = askNaturalNumber(sc, "How many users ? ");
        System.out.println("\n--- Generating " + nbUsers + " random users ---");

        for (int i = 0; i < nbUsers; i++) {
            mapManager.addUserPoint(new UserPoint(rand.nextInt(201) - 100, rand.nextInt(201) - 100));
        }

        return statistics(mapManager.getUserPoints(), mapManager.getSites());
    }

    /**
     * Performs a manual test by prompting the user for site and user coordinates.
     *
     * @param sc       The Scanner object for input.
     * @param sitesNb  The number of sites to define.
     * @return A Map mapping each Site ID to its total number of assigned users.
     */
    public static Map<Integer, Integer> manualTest(Scanner sc, int sitesNb) {
        mapManager.clear();

        System.out.println("\n--- Manual entry for " + sitesNb + " sites ---");
        for (int i = 0; i < sitesNb; i++) {
            System.out.println("Site #" + (i + 1) + ":");
            mapManager.addSite(new Site(askInt(sc, "Enter X: "), askInt(sc, "Enter Y: "), i + 1));
        }

        int nbUsers = askNaturalNumber(sc, "How many users ? ");
        System.out.println("\n--- Manual entry for " + nbUsers + " users ---");

        for (int i = 0; i < nbUsers; i++) {
            System.out.println("USER " + (i + 1) + ":");
            mapManager.addUserPoint(new UserPoint(askInt(sc, "Enter User X: "), askInt(sc, "Enter User Y: ")));
        }

        return statistics(mapManager.getUserPoints(), mapManager.getSites());
    }

    /**
     * Calculates and returns a summary of user assignments per site.
     *
     * @param users The list of users to analyze.
     * @param sites The list of all available sites.
     * @return A Map where the key is the Site ID and the value is the count of users assigned to it.
     */
    public static Map<Integer, Integer> statistics(List<UserPoint> users, List<Site> sites) {
        // Map to store: Site ID -> Count of users
        Map<Integer, Integer> stats = new HashMap<>();

        // Initialize map with 0 for all sites
        for (Site s : sites) {
            stats.put(s.getId(), 0);
        }

        // Count assignments
        for (UserPoint u : users) {
            if (u.getClosestSite() != null) {
                int siteId = u.getClosestSite().getId();
                stats.put(siteId, stats.getOrDefault(siteId, 0) + 1);
            }
        }

        return stats;
    }

    /**
     * Displays the statistics report in the console.
     *
     * @param stats The map containing assignments count per site ID.
     */
    public static void displayStats(Map<Integer, Integer> stats) {
        System.out.println("\n--- Assignment Statistics ---");
        stats.forEach((siteId, count) ->
                System.out.println("Site ID " + siteId + " : " + count + " user(s)"));
    }

    /**
     * Simple internal test to instantiate and print edges between the first two sites.
     *
     * @param sites The list of available sites.
     */
    private static void testEdge(List<Site> sites) {
        Site s1 = sites.get(0);
        Site s2 = sites.get(1);

        Edge edge1 = new Edge(s1, s2);
        Edge edge2 = new Edge(s2, s1);

        displayEdges(edge1, edge2);
    }

    /**
     * Displays the given edges in the console.
     *
     * @param e1 The first edge to display.
     * @param e2 The second edge to display.
     */
    public static void displayEdges(Edge e1, Edge e2) {
        System.out.println("\n--- Testing Edge ---");
        System.out.println("edge1 = " + e1);
        System.out.println("edge2 = " + e2);
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
     *
     * @param sc  The Scanner object.
     * @param msg The message prompt to display.
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
     * Prevents crashes on non-integer inputs.
     *
     * @param sc      The Scanner object.
     * @param message The prompt message to display.
     * @return A valid integer.
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