package pgl.app.io;

import org.json.JSONArray;
import org.json.JSONObject;
import pgl.app.model.MapManager;
import pgl.app.model.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Utility class responsible for importing and parsing OpenStreetMap (OSM) data
 * exported in JSON format (Overpass API style) into the application's road network model.
 * * <p>This importer processes raw OSM geographic data by projecting coordinates into
 * a local 2D coordinate system and reconstructing the topology of intersections and roads.</p>
 * * @author YourName
 * @version 1.0
 */
public class MapImporterOSM {

    /**
     * Scaling factor used to convert geographic coordinates (latitude/longitude)
     * into discrete integer coordinates for the application grid.
     */
    private static final double SCALE = 10000.0;

    /**
     * Reference latitude used as the origin (center point) for the local coordinate projection.
     */
    private static final double OFFSET_LAT = 49.03; // Center of Cergy

    /**
     * Reference longitude used as the origin (center point) for the local coordinate projection.
     */
    private static final double OFFSET_LON = 2.06;

    /**
     * Imports a road network from an OSM JSON file and populates the provided {@link MapManager}.
     * * <p>The parsing is executed in a two-pass process:
     * <ol>
     * <li><b>Pass 1: Node Extraction</b> — Parses OSM {@code node} elements, converts
     * GPS coordinates into local 2D {@link Point} objects using an offset and scale,
     * deduplicates overlapping coordinates via a cache, and registers them as intersections.</li>
     * <li><b>Pass 2: Way Linking</b> — Parses OSM {@code way} elements, resolves the sequence
     * of node references, and creates the actual road segments connecting the points.</li>
     * </ol>
     * </p>
     * * @param manager  The map manager where the newly created intersections and roads will be registered.
     * @param jsonFile The path to the source OSM JSON file containing the raw elements array.
     * @throws Exception If an I/O error occurs reading the file or if the JSON structure is malformed.
     */
    public static void importFromOSM(MapManager manager, Path jsonFile) throws Exception {
        String content = Files.readString(jsonFile);
        JSONObject root = new JSONObject(content);

        JSONArray elements = root.getJSONArray("elements");

        // Cache to ensure the uniqueness of Point instances
        Map<String, Point> intersectionCache = new HashMap<>();
        Map<Long, Point> osmNodeMap = new HashMap<>();

        // ---- STEP 1: Extraction and recording of nodes ----
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            if (element.getString("type").equals("node")) {
                long id = element.getLong("id");
                double lat = element.getDouble("lat");
                double lon = element.getDouble("lon");

                int x = (int) ((lon - OFFSET_LON) * SCALE);
                int y = (int) ((lat - OFFSET_LAT) * SCALE);

                String coordKey = x + "_" + y;
                Point point = intersectionCache.get(coordKey);

                if (point == null) {
                    point = new Point(x, y);
                    intersectionCache.put(coordKey, point);

                    manager.getRoadNetwork().addIntersection(point);
                }
                osmNodeMap.put(id, point);
            }
        }

        Random rand = new Random();

        // ---- STEP 2: Linking road segments ----
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            if (element.getString("type").equals("way")) {
                if (!element.has("nodes")) continue;
                JSONArray nodes = element.getJSONArray("nodes");

                for (int j = 0; j < nodes.length() - 1; j++) {
                    Point p1 = osmNodeMap.get(nodes.getLong(j));
                    Point p2 = osmNodeMap.get(nodes.getLong(j + 1));

                    if (p1 != null && p2 != null) {
                        double traffic = (rand.nextDouble() < 0.30) ? (1.5 + rand.nextDouble() * 2.5) : 1.0;
                        manager.addRoad(p1, p2, traffic);
                    }
                }
            }
        }
    }
}