package geomed.app.test;

import geomed.app.MapManager;
import geomed.app.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility data factory class responsible for provisioning randomized mock test structures.
 * <p>
 * This factory generates simulation instances of {@link Hospital} units, {@link VictimIncident}
 * occurrences, and matching interconnected {@link RoadEdge} paths. By design, this class is entirely
 * decoupled and agnostic of any visual toolkit or graphic interface subsystem, rendering it fully reusable
 * across lightweight command-line Console runtimes and full JavaFX GUI layout dashboard components.
 * </p>
 *
 * @version 1.0
 */
public class SimulationDataGenerator {

    /** Centralized random number generator source used to seed coordinates and weights. */
    private static final Random rand = new Random();

    /** Immutable local cache copy of all available standard medical qualification disciplines. */
    private static final MedicalSpecialty[] specialties = MedicalSpecialty.values();

    /**
     * Private constructor to prevent instantiation of this utility data factory.
     */
    private SimulationDataGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates an array list of randomized hospital structures.
     * <p>
     * Each simulated entity is configured with:
     * </p>
     * <ul>
     * <li>A random maximum intake capacity value constrained within the range [10, 100].</li>
     * <li>Planar geometric placement layout coordinates bound inside safe operational zones
     * (X: 50 to 700 px, Y: 50 to 600 px).</li>
     * <li>A default fallback {@link MedicalSpecialty#GENERAL} qualification combined with a secondary,
     * randomly selected medical discipline priority assignment.</li>
     * </ul>
     *
     * @param count     the specific quantity of structural hospital instances to instantiate
     * @param startId   the baseline auto-incrementing unique identifier index to start numbering from
     * @return a compiled list filled with unique, randomly distributed {@link Hospital} data model nodes
     */
    public static List<Hospital> generateRandomHospitals(int count, int startId) {
        List<Hospital> hospitals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int capacity = rand.nextInt(91) + 10;

            int x = rand.nextInt(650) + 50;
            int y = rand.nextInt(550) + 50;
            Hospital h = new Hospital(x, y, startId + i, capacity);

            MedicalSpecialty randSpec = specialties[rand.nextInt(specialties.length)];
            h.addSpecialty(randSpec);
            h.addSpecialty(MedicalSpecialty.GENERAL);

            hospitals.add(h);
        }
        return hospitals;
    }

    /**
     * Generates a batch collection of randomized emergency victim incidents.
     * <p>
     * Every simulated incident event is seeded with:
     * </p>
     * <ul>
     * <li>A padded alphanumeric structural identifier format string (e.g., {@code INC-R-001}).</li>
     * <li>A single specialized primary medical triage emergency requirement category type.</li>
     * <li>A 20% mathematical probability of configuring a fixed preferred destination hospital matching
     * an item from the supplied available hubs list.</li>
     * </ul>
     *
     * @param count            the explicit quantity of mock victim emergency incidents to create
     * @param startCount       the numerical tracking offset used to format ordered serial tracking identifier tags
     * @param currentHospitals the active cluster of baseline hospital nodes currently registered in the topology map
     * @return a collection array consisting of randomized, unmapped {@link VictimIncident} emergency contexts
     */
    public static List<VictimIncident> generateRandomIncidents(int count, int startCount, List<Hospital> currentHospitals) {
        List<VictimIncident> incidents = new ArrayList<>();
        int nbHospitals = currentHospitals.size();

        for (int i = 0; i < count; i++) {
            String incidentId = "INC-R-" + String.format("%03d", startCount + i);
            MedicalSpecialty type = specialties[rand.nextInt(specialties.length)];

            Integer prefId = null;
            if (nbHospitals > 0 && rand.nextDouble() < 0.20) {
                Hospital randomHosp = currentHospitals.get(rand.nextInt(nbHospitals));
                prefId = randomHosp.getId();
            }

            int incidentX = rand.nextInt(650) + 50;
            int incidentY = rand.nextInt(550) + 50;
            incidents.add(new VictimIncident(incidentX, incidentY, incidentId, type, prefId));
        }
        return incidents;
    }

    /**
     * Injects a randomized, multi-weighted grid network of roads interconnecting registered active hospital nodes.
     * <p>
     * <b>Traffic Congestion Probability Distribution Model:</b>
     * </p>
     * <ul>
     * <li>There is a 30% statistical probability that a path experiences congestion issues.
     * The resulting traffic factor metric dynamically scales within the range [1.5, 4.0).</li>
     * <li>The remaining 70% of connections map as standard fluid traffic links, carrying a baseline traffic factor value of exactly 1.0.</li>
     * </ul>
     * <p>
     * Links connecting a single hospital node back onto itself are intercepted and discarded to prevent infinite routing self-loops.
     * </p>
     *
     * @param mapManager the underlying target central core orchestrator system context instance to inject roads into
     * @param nbRoads    the total target quantity of cross-grid connective road segments to generate
     */
    public static void generateRandomRoads(MapManager mapManager, int nbRoads) {
        List<Hospital> hospitals = new ArrayList<>(mapManager.getSites());
        if (hospitals.size() < 2) return;

        for (int i = 0; i < nbRoads; i++) {
            Hospital h1 = hospitals.get(rand.nextInt(hospitals.size()));
            Hospital h2 = hospitals.get(rand.nextInt(hospitals.size()));

            if (h1 != h2) {
                double traffic = (rand.nextDouble() < 0.30) ? (1.5 + rand.nextDouble() * 2.5) : 1.0;
                mapManager.addRoad(new Point(h1.getX(), h1.getY()), new Point(h2.getX(), h2.getY()), traffic);
            }
        }
    }
}