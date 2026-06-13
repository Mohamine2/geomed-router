package pgl.app.io;

import pgl.app.model.VictimIncident;
import pgl.app.model.MedicalSpecialty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class responsible for importing victim incident data from a CSV file.
 */
public class CsvIncidentImporter {

    /**
     * Parses a CSV file containing incident (victim) data and converts it into a list of {@link VictimIncident}.
     * <p>The expected format for each row (with or without a header) is:
     * {@code X; Y; EmergencyType; PreferredHospitalId} or {@code X, Y, EmergencyType, PreferredHospitalId}.</p>
     * <p><b>Parsing rules:</b></p>
     * <ul>
     * <li>Empty lines are ignored.</li>
     * <li>The separator can be a comma (,) or a semicolon (;).</li>
     * <li>The first line is automatically skipped if it is identified as a header.</li>
     * <li>If the emergency type is missing or invalid, it defaults to {@link MedicalSpecialty#GENERAL}.</li>
     * <li>If the preferred hospital ID is missing, invalid (e.g. "N/A"), or <= 0, it defaults to null.</li>
     * <li>Rows with invalid coordinates are skipped and an error is logged.</li>
     * </ul>
     *
     * @param filePath   the {@link Path} to the CSV file to be read
     * @param startCount the starting number used to generate unique incident IDs
     * @return a {@link List} of parsed {@link VictimIncident} objects
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static List<VictimIncident> importFromCsv(Path filePath, int startCount) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        List<VictimIncident> incidents = new ArrayList<>();
        int currentCount = startCount;
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

                // Retrieve the medical specialty, defaulting to GENERAL
                MedicalSpecialty emergencyType = MedicalSpecialty.GENERAL;
                if (parts.length > 2) {
                    try {
                        emergencyType = MedicalSpecialty.valueOf(parts[2].trim().toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }

                Integer prefId = null;
                if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                    try {
                        int parsedId = Integer.parseInt(parts[3].trim());
                        if (parsedId > 0) {
                            prefId = parsedId;
                        }
                    } catch (NumberFormatException ignored) {
                        // Error tolerance: If the CSV contains "N/A" or text,
                        // it is silently ignored and prefId remains null.
                    }
                }

                // Generate a unique ID for the CSV import
                String incidentId = "INC-CSV-" + String.format("%03d", currentCount++);

                incidents.add(new VictimIncident(x, y, incidentId, emergencyType, prefId));

            } catch (NumberFormatException e) {
                System.err.println("Skipped CSV line (invalid numerical coordinates): " + line);
            }
        }
        return incidents;
    }
}