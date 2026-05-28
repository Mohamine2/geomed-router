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

    /**
     * The list of sites currently loaded or generated in the session.
     */
    private static List<Site> currentSites = new ArrayList<>();

    /**
     * The list of users currently loaded or generated in the session.
     */
    private static List<UserPoint> currentUsers = new ArrayList<>();

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
            System.out.println("3. Test Triangle & Geometry (using current points)");
            System.out.println("4. Exit");
            System.out.print("Choice: ");

            if (sc.hasNextInt()) {
                int mode = sc.nextInt();
                switch (mode) {
                    case 1:
                        displayStats(randomTest(sc, askNaturalNumber(sc, "How many sites (You need at least 3 sites to create a triangle) ? ")));
                        break;
                    case 2:
                        displayStats(manualTest(sc, askNaturalNumber(sc, "How many sites (You need at least 3 sites to create a triangle) ? ")));
                        break;
                    case 3:
                        testTriangleAndGeometry();
                        break;
                    case 4:
                        running = false;
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option! Please enter a number between 1 and 4.");
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
        List<Site> sites = new ArrayList<>();

        System.out.println("\n--- Generating " + nbSites + " random sites ---");

        for (int i = 0; i < nbSites; i++) {
            // Generating coordinates between -100 and 100
            sites.add(new Site(rand.nextInt(201) - 100, rand.nextInt(201) - 100, i + 1));
        }

        int nbUsers = askNaturalNumber(sc, "How many users ? ");
        System.out.println("\n--- Generating " + nbUsers + " random users ---");

        List<UserPoint> users = new ArrayList<>();

        for(int i = 0; i < nbUsers; i++){
            UserPoint user = new UserPoint(rand.nextInt(201) - 100, rand.nextInt(201) - 100);
            users.add(user);
            Site closest = findClosestSite(user, sites);
            user.setClosestSite(closest);
        }

        // Global save for Option 3
        currentSites = sites;
        currentUsers = users;

        if (sites.size() >= 2) {
            testEdge(sites);
        }

        return statistics(users, sites);
    }

    /**
     * Performs a manual test by prompting the user for site and user coordinates.
     *
     * @param sc       The Scanner object for input.
     * @param sitesNb  The number of sites to define.
     * @return A Map mapping each Site ID to its total number of assigned users.
     */
    public static Map<Integer, Integer> manualTest(Scanner sc, int sitesNb) {
        List<Site> sites = new ArrayList<>();

        System.out.println("\n--- Manual entry for " + sitesNb + " sites ---");
        for (int i = 0; i < sitesNb; i++) {
            System.out.println("Site #" + (i + 1) + ":");
            int x = askInt(sc, "Enter X: ");
            int y = askInt(sc, "Enter Y: ");
            sites.add(new Site(x, y, i + 1));
        }

        int nbUsers = askNaturalNumber(sc, "How many users ? ");
        System.out.println("\n--- Manual entry for " + nbUsers + " users ---");

        List<UserPoint> users = new ArrayList<>();

        for(int i = 0; i < nbUsers; i++){
            System.out.println("USER " + (i + 1) + ":");
            UserPoint user = new UserPoint(askInt(sc, "Enter User X: "), askInt(sc, "Enter User Y: "));
            users.add(user);
            Site closest = findClosestSite(user, sites);
            user.setClosestSite(closest);
        }

        // Global save for Option 3
        currentSites = sites;
        currentUsers = users;

        if (sites.size() >= 2) {
            testEdge(sites);
        }

        return statistics(users, sites);
    }

    /**
     * Logic to find and return the closest site for a given user based on squared distance.
     *
     * @param user  The UserPoint to evaluate.
     * @param sites The list of available sites.
     * @return The closest Site object, or null if the sites list is empty.
     */
    public static Site findClosestSite(UserPoint user, List<Site> sites) {
        Site closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Site site : sites) {
            double dist = user.distanceSquaredTo(site.getX(), site.getY());
            if (dist < minDistance) {
                minDistance = dist;
                closest = site;
            }
        }
        return closest;
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
     * Displays the results of the circumcircle and triangle test in the console.
     * * @param triangle    The Triangle object being evaluated.
     * @param testResults A Map containing point descriptions and their inclusion status.
     */
    public static void displayTriangleResults(Triangle triangle, Map<String, Boolean> testResults) {
        System.out.println("\n--- Résultats du Test Triangle & GeometryUtils ---");

        Point center = triangle.getCircumcenter();
        System.out.println("Centre du cercle circonscrit calculé : (" + center.getX() + ", " + center.getY() + ")");
        System.out.println("\n--- Test du prédicat In-Circle ---");

        testResults.forEach((pointDesc, isInside) -> {
            System.out.println(pointDesc + " est dans le cercle ? " + isInside);
        });

        System.out.println("----------------------------------------");
    }

    /**
     * Evaluates which sites and users lie within the circumcircle of the given triangle.
     * * @param triangle     The Triangle whose circumcircle is used for the inclusion test.
     * @param sitesToTest  The list of sites to evaluate.
     * @param usersToTest  The list of users to evaluate.
     * @return An ordered Map linking the point description/ID to a Boolean (true if inside the circumcircle).
     */
    public static Map<String, Boolean> evaluateInCircle(Triangle triangle, List<Site> sitesToTest, List<UserPoint> usersToTest) {
        Map<String, Boolean> results = new LinkedHashMap<>(); // LinkedHashMap to preserve insertion order

        // Sites' test
        for (Site s : sitesToTest) {
            boolean inside = triangle.containsInCircumcircle(s);
            results.put("Site ID " + s.getId() + " (" + s.getX() + ", " + s.getY() + ")", inside);
        }

        // Users' test
        for (UserPoint u : usersToTest) {
            boolean inside = triangle.containsInCircumcircle(u);
            results.put("UserPoint (" + u.getX() + ", " + u.getY() + ")", inside);
        }

        return results;
    }

    /**
     * Tests the geometric properties of a triangle created from the first three stored sites.
     * Evaluates whether remaining sites and a subset of users are inside its circumcircle.
     */
    private static void testTriangleAndGeometry() {
        if (currentSites.size() < 3) {
            System.out.println("Erreur : Pas assez de sites en mémoire !");
            return;
        }

        Site p1 = currentSites.get(0);
        Site p2 = currentSites.get(1);
        Site p3 = currentSites.get(2);
        Triangle triangle = new Triangle(p1, p2, p3);

        List<Site> sitesToTest = currentSites.subList(3, currentSites.size());
        List<UserPoint> usersToTest = currentUsers.subList(0, Math.min(5, currentUsers.size()));

        Map<String, Boolean> results = evaluateInCircle(triangle, sitesToTest, usersToTest);

        displayTriangleResults(triangle, results);
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