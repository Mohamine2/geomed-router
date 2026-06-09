package pgl.app.io;

import org.json.JSONArray;
import org.json.JSONObject;
import pgl.app.model.MapManager;
import pgl.app.model.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MapImporterOSM {

    // Facteurs de projection pour Cergy (Latitude/Longitude vers tes coordonnées X/Y)
    // À ajuster selon tes besoins pour que la carte soit bien centrée
    private static final double SCALE = 10000.0;
    private static final double OFFSET_LAT = 49.03; // Latitude centre Cergy
    private static final double OFFSET_LON = 2.06;  // Longitude centre Cergy

    public static void importFromOSM(MapManager manager, Path jsonFile) throws Exception {
        String content = Files.readString(jsonFile);
        JSONObject root = new JSONObject(content);
        JSONArray elements = root.getJSONArray("elements");

        Map<Long, Point> nodeMap = new HashMap<>();

        // 1. Première passe : On enregistre tous les nœuds (coordonnées)
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            if (element.getString("type").equals("node")) {
                long id = element.getLong("id");
                double lat = element.getDouble("lat");
                double lon = element.getDouble("lon");

                // Conversion Lat/Lon en X/Y
                int x = (int) ((lon - OFFSET_LON) * SCALE);
                int y = (int) ((lat - OFFSET_LAT) * SCALE);

                nodeMap.put(id, new Point(x, y));
            }
        }

        // 2. Deuxième passe : On crée les routes à partir des chemins (ways)
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            if (element.getString("type").equals("way")) {
                JSONArray nodes = element.getJSONArray("nodes");
                for (int j = 0; j < nodes.length() - 1; j++) {
                    Point p1 = nodeMap.get(nodes.getLong(j));
                    Point p2 = nodeMap.get(nodes.getLong(j + 1));

                    if (p1 != null && p2 != null) {
                        manager.addRoad(p1, p2);
                    }
                }
            }
        }
    }
}