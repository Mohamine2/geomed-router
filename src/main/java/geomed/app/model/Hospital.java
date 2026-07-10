package geomed.app.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a medical facility on the map.
 * This class extends {@link Site} to inherit geometric properties (x, y, id) 
 * and adds business logic specific to hospital management such as capacity, 
 * saturation levels, and medical specialties.
 *
 * @version 2.0
 */
public class Hospital extends Site {

    /** The maximum number of patients the hospital can accommodate. */
    private final int capacityMax;
    
    /** The current number of patients admitted to the hospital. */
    private int currentPatients;

    /** A set of medical specialties available at the hospital. */
    private final Set<MedicalSpecialty> specialties;

    /** The threshold (e.g., 0.90 for 90%) at which the hospital is considered saturated. */
    private static final double SATURATION_THRESHOLD = 0.90;

    /**
     * Constructs a new Hospital with specified coordinates, identifier, and maximum capacity.
     *
     * @param x           The X coordinate of the hospital on the map.
     * @param y           The Y coordinate of the hospital on the map.
     * @param id          The unique identifier of the hospital.
     * @param capacityMax The maximum patient capacity (must be strictly positive).
     * @throws IllegalArgumentException if capacityMax is less than or equal to zero.
     */
    public Hospital(double x, double y, int id, int capacityMax) {
        super(x, y, id); // Inherit geometric properties from Site
        
        if (capacityMax <= 0) {
            throw new IllegalArgumentException("Hospital capacity must be strictly positive.");
        }
        
        this.capacityMax = capacityMax;
        this.currentPatients = 0;
        this.specialties = new HashSet<>();
    }

    /**
     * Calculates the current occupancy rate of the hospital.
     *
     * @return A double value between 0.0 (empty) and 1.0 (full capacity).
     */
    public double getOccupancyRate() {
        return (double) currentPatients / capacityMax;
    }

    /**
     * Checks whether the hospital has reached or exceeded its saturation threshold.
     *
     * @return {@code true} if the hospital is saturated, {@code false} otherwise.
     */
    public boolean isSaturated() {
        return getOccupancyRate() >= SATURATION_THRESHOLD;
    }
    /**
     * Attempts to admit a new patient to the hospital.
     *
     * @return {@code true} if the patient was successfully admitted, 
     * {@code false} if the hospital is already at full capacity.
     */
    public boolean admitPatient() {
        if (currentPatients < capacityMax) {
            currentPatients++;
            return true;
        }
        return false; 
    }

    /**
     * Discharges a patient from the hospital, freeing up one bed.
     * If the hospital is already empty, this method does nothing.
     */
    public void dischargePatient() {
        if (currentPatients > 0) {
            currentPatients--;
        }
    }

    /**
     * Adds a medical specialty to the hospital's capabilities.
     *
     * @param specialty The {@link MedicalSpecialty} to add.
     */
    public void addSpecialty(MedicalSpecialty specialty) {
        if (specialty != null) {
            this.specialties.add(specialty);
        }
    }

    /**
     * Checks if the hospital is equipped to treat a specific type of medical emergency.
     *
     * @param emergencyType The required medical specialty.
     * @return {@code true} if the hospital has the specialty, {@code false} otherwise.
     */
    public boolean canTreat(MedicalSpecialty emergencyType) {
        if (emergencyType == null) {
            return false;
        }
        return this.specialties.contains(emergencyType);
    }

    /**
     * Gets the maximum capacity of the hospital.
     *
     * @return The maximum number of patients.
     */
    public int getCapacityMax() { 
        return capacityMax; 
    }

    /**
     * Gets the current number of patients in the hospital.
     *
     * @return The current patient count.
     */
    public int getCurrentPatients() { 
        return currentPatients; 
    }

    /**
     * Retrieves an unmodifiable view of the hospital's medical specialties.
     * This defensive copy prevents external modification of the internal set.
     *
     * @return An unmodifiable Set containing the specialties.
     */
    public Set<MedicalSpecialty> getSpecialties() {
        return Set.copyOf(specialties);
    }
}