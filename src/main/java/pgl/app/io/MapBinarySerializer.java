package pgl.app.io;

import pgl.app.algo.exception.HospitalCollisionException;
import pgl.app.model.Hospital;
import pgl.app.model.MapManager;
import pgl.app.model.MedicalSpecialty;
import pgl.app.model.Point;
import pgl.app.model.RoadEdge;
import pgl.app.model.VictimIncident;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Serializes and deserializes a complete map to a binary file.
 * <p>
 * Format: intersections (graph vertices), roads (graph edges), hospitals, incidents.
 * Delaunay triangles are recomputed after import via {@link MapManager#updateAll()}.
 * </p>
 */
public final class MapBinarySerializer {

    /** Magic number "PGLM". */
    public static final int MAGIC = 0x50474C4D;
    public static final short VERSION = 1;
    public static final String FILE_EXTENSION = "pglm";

    private MapBinarySerializer() {
    }

    public static void exportToFile(MapManager manager, Path file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) {
            export(manager, out);
        }
    }

    public static void importFromFile(MapManager manager, Path file) throws IOException, HospitalCollisionException{
        try (InputStream in = Files.newInputStream(file)) {
            importMap(manager, in);
        }
    }

    public static void export(MapManager manager, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        data.writeInt(MAGIC);
        data.writeShort(VERSION);

        List<Point> intersections = manager.getRoadNetwork().getIntersections();
        data.writeInt(intersections.size());
        for (Point p : intersections) {
            data.writeDouble(p.getX());
            data.writeDouble(p.getY());
        }

        List<RoadEdge> roads = manager.getRoadNetwork().getRoads();
        data.writeInt(roads.size());
        for (RoadEdge road : roads) {
            data.writeInt(indexOf(intersections, road.getStart()));
            data.writeInt(indexOf(intersections, road.getEnd()));
            data.writeDouble(road.getTrafficFactor());
        }

        Set<Hospital> hospitals = manager.getSites();
        data.writeInt(hospitals.size());
        for (Hospital hospital : hospitals) {
            data.writeInt(hospital.getId());
            data.writeDouble(hospital.getX());
            data.writeDouble(hospital.getY());
            data.writeInt(hospital.getCapacityMax());
            data.writeInt(hospital.getCurrentPatients());
            MedicalSpecialty[] specialties = hospital.getSpecialties().toArray(new MedicalSpecialty[0]);
            data.writeInt(specialties.length);
            for (MedicalSpecialty specialty : specialties) {
                data.writeUTF(specialty.name());
            }
        }

        List<VictimIncident> incidents = manager.getIncidents();
        data.writeInt(incidents.size());
        for (VictimIncident incident : incidents) {
            data.writeDouble(incident.getX());
            data.writeDouble(incident.getY());
            data.writeUTF(incident.getIncidentId());
            data.writeByte((byte) incident.getEmergencyType().ordinal());
            Integer preferred = incident.getPreferredHospitalId();
            data.writeInt(preferred == null ? -1 : preferred);
        }

        data.flush();
    }

    public static void importMap(MapManager manager, InputStream in) throws IOException, HospitalCollisionException {
        DataInputStream data = new DataInputStream(in);

        int magic = data.readInt();
        if (magic != MAGIC) {
            throw new IOException("Invalid map file: bad magic number.");
        }

        short version = data.readShort();
        if (version != VERSION) {
            throw new IOException("Unsupported map file version: " + version);
        }

        manager.clear();

        int intersectionCount = data.readInt();
        for (int i = 0; i < intersectionCount; i++) {
            double x = data.readDouble();
            double y = data.readDouble();
            manager.getRoadNetwork().addIntersection(new Point(x, y));
        }

        int roadCount = data.readInt();
        for (int i = 0; i < roadCount; i++) {
            int startIdx = data.readInt();
            int endIdx = data.readInt();
            double traffic = data.readDouble();
            validateIndex(startIdx, intersectionCount, "road start");
            validateIndex(endIdx, intersectionCount, "road end");
            RoadEdge road = manager.addRoad(startIdx, endIdx);
            road.setTrafficFactor(traffic);
        }

        int hospitalCount = data.readInt();
        for (int i = 0; i < hospitalCount; i++) {
            int id = data.readInt();
            double x = data.readDouble();
            double y = data.readDouble();
            int capacityMax = data.readInt();
            int currentPatients = data.readInt();
            int specialtyCount = data.readInt();

            Hospital hospital = new Hospital(x, y, id, capacityMax);
            for (int s = 0; s < specialtyCount; s++) {
                hospital.addSpecialty(readSpecialty(data.readByte()));
            }
            for (int p = 0; p < currentPatients; p++) {
                hospital.admitPatient();
            }
            manager.addHospital(hospital);
        }

        int incidentCount = data.readInt();
        for (int i = 0; i < incidentCount; i++) {
            double x = data.readDouble();
            double y = data.readDouble();
            String incidentId = data.readUTF();

            byte specialtyOrdinal = data.readByte();
            MedicalSpecialty type = readSpecialty(specialtyOrdinal);

            int preferredId = data.readInt();
            VictimIncident incident = preferredId < 0
                    ? new VictimIncident(x, y, incidentId, type)
                    : new VictimIncident(x, y, incidentId, type, preferredId);
            manager.addIncident(incident);
        }

        manager.updateAll();
    }

    private static int indexOf(List<Point> intersections, Point target) {
        for (int i = 0; i < intersections.size(); i++) {
            if (intersections.get(i) == target) {
                return i;
            }
        }
        throw new IllegalStateException("Road endpoint not found in intersection list.");
    }

    private static void validateIndex(int index, int size, String field) throws IOException {
        if (index < 0 || index >= size) {
            throw new IOException("Invalid " + field + " index: " + index + " (size=" + size + ")");
        }
    }

    private static MedicalSpecialty readSpecialty(byte ordinal) throws IOException {
        MedicalSpecialty[] values = MedicalSpecialty.values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IOException("Unknown medical specialty ordinal: " + ordinal);
        }
        return values[ordinal];
    }
}
