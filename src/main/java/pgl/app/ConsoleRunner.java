package pgl.app;

import pgl.app.model.Site;
import pgl.app.model.Edge;
import pgl.app.model.UserPoint;

import java.util.*;

/**
 * The ConsoleRunner class serves as the command-line interface entry point for the application.
 * It provides a robust interface to test the logic (Voronoi/Delaunay related models)
 * without requiring the JavaFX graphical interface.
 * @version 1.0
 */
public class ConsoleRunner {
	
	private static void testEdge() {
	    System.out.println("\n--- Testing Edge ---");

	    Site site1 = new Site(1.0, 2.0, 1);
	    Site site2 = new Site(3.0, 4.0, 2);

	    Edge edge1 = new Edge(site1, site2);
	    Edge edge2 = new Edge(site2, site1);

	    System.out.println("edge1 = " + edge1);
	    System.out.println("edge2 = " + edge2);
	    System.out.println("edge1 equals edge2: " + edge1.equals(edge2));
	    System.out.println("same hashCode: " + (edge1.hashCode() == edge2.hashCode()));
	}

    /**
     * Main entry point for the console application.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("--- Starting MVP Test: Console Mode ---");
        testEdge(); //test temporaire testeEdge();
        
        boolean running = true;
        while (running) {
            System.out.println("\nSelect an option:");
            System.out.println("1. Automated Test (Random)");
            System.out.println("2. Manual Test");
            System.out.println("3. Exit");
            System.out.print("Choice: ");

            if (sc.hasNextInt()) {
                int mode = sc.nextInt();
                switch (mode) {
                    case 1:
                        displayStats(randomTest(sc, askNaturalNumber(sc, "How many sites?")));
                        break;
                    case 2:
                        displayStats(manualTest(sc, askNaturalNumber(sc, "How many sites? ")));
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
     * Performs an automated test by generating random sites and random user points.
     *
     * @param sc       The Scanner object for input.
     * @param nbSites  The number of sites to generate.
     * @return hashMap summary
     */
    public static Map<Integer, Integer> randomTest(Scanner sc, int nbSites) {
        Random rand = new Random();
        List<Site> sites = new ArrayList<>();

        System.out.println("\n--- Generating " + nbSites + " random sites ---");

        for (int i = 0; i < nbSites; i++) {
            // Generating coordinates between -100 and 100
            sites.add(new Site(rand.nextInt(201) - 100, rand.nextInt(201) - 100, i + 1));
        }

        int nbUsers = askNaturalNumber(sc, "How many users ?");
        System.out.println("\n--- Generating " + nbUsers + " random users ---");

        List<UserPoint> users = new ArrayList<>();

        for(int i = 0; i < nbUsers; i++){
            UserPoint user = new UserPoint(rand.nextInt(201) - 100, rand.nextInt(201) - 100);
            users.add(user);
            Site closest = findClosestSite(user, sites);
            user.setClosestSite(closest);
        }

        return statistics(users, sites);
    }

    /**
     * Performs a manual test by prompting the user for site and user coordinates.
     *
     * @param sc       The Scanner object for input.
     * @param sitesNb  The number of sites to define.
     * @return hashMap summary
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

        int nbUsers = askNaturalNumber(sc, "How many users ?");
        System.out.println("\n--- Manual entry for " + nbUsers + " users ---");

        List<UserPoint> users = new ArrayList<>();

        for(int i = 0; i < nbUsers; i++){
            System.out.println("USER " + (i + 1) + ":");
            UserPoint user = new UserPoint(askInt(sc, "Enter User X: "), askInt(sc, "Enter User Y: "));
            users.add(user);
            Site closest = findClosestSite(user, sites);
            user.setClosestSite(closest);
        }

        return statistics(users, sites);
    }

    /**
     * Logic to find and return the closest site for a given user.
     *
     * @param user  The UserPoint to evaluate.
     * @param sites The list of available sites.
     * @return closest The closest site
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
     * @return stats The hashMap summary
     */
    public static Map<Integer,Integer> statistics(List<UserPoint> users, List<Site> sites) {
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
     * Helper method to robustly ask for a natural number.
     *
     * @param sc The Scanner object.
     * @return A valid integer greater than 0.
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