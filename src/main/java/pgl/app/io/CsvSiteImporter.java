package pgl.app.io;

import pgl.app.model.Hospital;
import pgl.app.model.MedicalSpecialty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class responsible for importing hospital site data from a CSV file.
 */
public class CsvSiteImporter {

    /**
     * Parses a CSV file containing hospital reference site data and converts it into a list of {@link Hospital}.
     * * <p>The expected format for each row (with or without a header) is:
     * {@code X, Y, Capacity, Specialty1-Specialty2-...}</p>
     * * <p><b>Parsing rules:</b></p>
     * <ul>
     * <li>Empty lines are ignored.</li>
     * <li>The column separator can be a comma (,) or a semicolon (;).</li>
     * <li>The first line is automatically skipped if identified as a header (contains alphabetical characters in the first column).</li>
     * <li>The default capacity is 100 if not specified.</li>
     * <li>Specialties should be separated by a hyphen (-).</li>
     * <li>If no specialties are provided, {@link MedicalSpecialty#GENERAL} is assigned by default.</li>
     * <li>Rows with invalid coordinates or capacities are skipped and an error is logged.</li>
     * </ul>
     *
     * @param filePath the {@link Path} to the CSV file to be read
     * @param startId  the starting ID value to be assigned to the imported hospitals
     * @return a {@link List} of parsed {@link Hospital} objects
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static List<Hospital> importFromCsv(Path filePath, int startId) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        List<Hospital> hospitals = new ArrayList<>();
        int currentId = startId;
        boolean firstLine = true;

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split("[,;]");

            if (firstLine) {
                firstLine = false;
                // Ignore the header if there are letters in the first column
                if (parts[0].matches(".*[a-zA-Z].*")) continue;
            }

            try {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                int capacity = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 100; // Default capacity

                Hospital h = new Hospital(x, y, currentId++, capacity);

                // Add specialties if provided (e.g., CARDIOLOGY-NEUROLOGY)
                if (parts.length > 3) {
                    String[] specialties = parts[3].split("-");
                    for (String spec : specialties) {
                        try {
                            h.addSpecialty(MedicalSpecialty.valueOf(spec.trim().toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                            // Ignore typos in specialty names
                        }
                    }
                } else {
                    h.addSpecialty(MedicalSpecialty.GENERAL);
                }

                hospitals.add(h);
            } catch (NumberFormatException e) {
                System.err.println("Skipped CSV line (invalid numerical data): " + line);
            }
        }
        return hospitals;
    }
}