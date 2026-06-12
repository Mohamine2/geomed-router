package pgl.app.test;

import pgl.app.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Usine chargée de générer des données de test et de simulation (Hôpitaux, Incidents, Routes).
 * Cette classe est agnostique de l'UI (utilisable en Console et JavaFX).
 */
public class SimulationDataGenerator {

    private static final Random rand = new Random();
    private static final MedicalSpecialty[] specialties = MedicalSpecialty.values();

    /**
     * Génère une liste d'hôpitaux aléatoires.
     */
    public static List<Hospital> generateRandomHospitals(int count, int startId) {
        List<Hospital> hospitals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int capacity = rand.nextInt(91) + 10;

            int x = rand.nextInt(650) + 50; // Position X aléatoire entre 50 et 700
            int y = rand.nextInt(550) + 50; // Position Y aléatoire entre 50 et 600
            Hospital h = new Hospital(x, y, startId + i, capacity);

            MedicalSpecialty randSpec = specialties[rand.nextInt(specialties.length)];
            h.addSpecialty(randSpec);
            h.addSpecialty(MedicalSpecialty.GENERAL);

            hospitals.add(h);
        }
        return hospitals;
    }

    /**
     * Génère une liste d'incidents aléatoires.
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

            // Pour la génération des Incidents :
            int incidentX = rand.nextInt(650) + 50;
            int incidentY = rand.nextInt(550) + 50;
            incidents.add(new VictimIncident(incidentX, incidentY, incidentId, type, prefId));
        }
        return incidents;
    }

    /**
     * Génère un réseau routier aléatoire entre les hôpitaux existants.
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