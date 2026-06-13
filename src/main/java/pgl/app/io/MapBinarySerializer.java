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

    /** * Current version of the binary file format.
     */
    public static final short VERSION = 1;

    /** * Standard file extension for the application's maps.
     */
    public static final String FILE_EXTENSION = "pglm";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MapBinarySerializer() {
    }

    /**
     * Exports the current state of the map manager to a file on the disk.
     *
     * @param manager The map manager containing the data to save.
     * @param file    The target file path where the data will be written.
     * @throws IOException If an input/output error occurs during file creation or writing.
     */
    public static void exportToFile(MapManager manager, Path file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) {
            export(manager, out);
        }
    }

    /**
     * Imports map data from a binary file and loads it into the manager.
     *
     * @param manager The map manager that will receive the imported data.
     * @param file    The path of the binary file to read.
     * @throws IOException                If a reading error occurs, or if the file format/version is invalid.
     * @throws HospitalCollisionException If the imported data causes a spatial collision between multiple hospitals.
     */
    public static void importFromFile(MapManager manager, Path file) throws IOException, HospitalCollisionException{
        try (InputStream in = Files.newInputStream(file)) {
            importMap(manager, in);
        }
    }

    /**
     * Serializes the model content (intersections, roads, hospitals, incidents) into a binary output stream.
     *
     * @param manager The map manager providing the data.
     * @param out     The output stream where the binary data will be written.
     * @throws IOException If an error occurs while writing to the stream.
     */
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
                data.writeByte((byte) specialty.ordinal());
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

    /**
     * Deserializes and reconstructs the map state from a binary input stream.
     * Handles the reading of elements, their re-instantiation, and post-import geometric calculations.
     *
     * @param manager The map manager to update.
     * @param in      The input stream containing the formatted data.
     * @throws IOException                If the file is corrupted, has a bad magic number, an unsupported version, or in case of a reading error.
     * @throws HospitalCollisionException If the reconstruction leads to overlapping hospitals.
     */
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

            manager.addRoad(startIdx, endIdx, traffic);
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

            int rawPrefId = data.readInt();

            // We directly translate the -1 from the binary file to 'null' Java
            Integer preferredId = (rawPrefId < 0) ? null : rawPrefId;

            VictimIncident incident = new VictimIncident(x, y, incidentId, type, preferredId);

            manager.addIncident(incident);
        }

        manager.updateAll();
    }

    /**
     * Finds the index of a specific point (intersection) within a list.
     * This method is used to reference road endpoints by their index.
     *
     * @param intersections The list of all available points/intersections.
     * @param target        The exact point to search for.
     * @return The integer index of the point in the list.
     * @throws IllegalStateException If the searched point is not found in the list.
     */
    private static int indexOf(List<Point> intersections, Point target) {
        for (int i = 0; i < intersections.size(); i++) {
            if (intersections.get(i).equals(target)) {
                return i;
            }
        }
        throw new IllegalStateException("Road endpoint not found in intersection list.");
    }

    /**
     * Validates that an index read from the binary file is within the expected bounds.
     * Prevents OutOfBounds errors if the file is altered.
     *
     * @param index The index to validate.
     * @param size  The maximum size of the structure (exclusive upper bound).
     * @param field A textual description of the field (for a clearer error message).
     * @throws IOException If the index is strictly less than 0 or greater than/equal to the size limit.
     */
    private static void validateIndex(int index, int size, String field) throws IOException {
        if (index < 0 || index >= size) {
            throw new IOException("Invalid " + field + " index: " + index + " (size=" + size + ")");
        }
    }

    /**
     * Converts a byte value read from the file into the corresponding {@link MedicalSpecialty} enumeration.
     *
     * @param ordinal The numeric ordinal of the saved medical specialty.
     * @return The medical specialty associated with this ordinal.
     * @throws IOException If the read value does not correspond to any known specialty (possibly corrupted file or mismatched version).
     */
    private static MedicalSpecialty readSpecialty(byte ordinal) throws IOException {
        MedicalSpecialty[] values = MedicalSpecialty.values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IOException("Unknown medical specialty ordinal: " + ordinal);
        }
        return values[ordinal];
    }
}