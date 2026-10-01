package model;

import enums.SpaceType;
import enums.VehicleType;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import pricing.PricingPolicy;

/**
 * Common abstraction for every vehicle that can use the parking lot.
 * Each subclase only states what makes it different: the space it needs
 * and the pricing policy it uses. The fee calculation itself is final and
 * delegates to that policy, so nobody outside needs to ask what kind of
 * vehicle this is.
 */
public abstract class Vehicle {

    private final String licensePlate;
    private final String brand;
    private final String model;
    private final String color;
    private final VehicleType type;

    protected Vehicle(String licensePlate, String brand, String model, String color, VehicleType type) {
        this.licensePlate = normalizePlate(licensePlate);
        this.brand = requireText(brand, "Brand");
        this.model = requireText(model, "Model");
        this.color = requireText(color, "Color");
        this.type = Objects.requireNonNull(type, "Vehicle type must not be null.");
    }

    /**
     * Calculates how much this vehicle pays for a stay (polymorphism).
     */
    public final long calculateFee(Duration stay) {
        return getPricingPolicy().calculateFee(stay);
    }

        /**
     * Calculates how many hours this vehicle is charged for a stay.
     */
    public final long calculateChargeableHours(Duration stay) {
        return getPricingPolicy().calculateChargeableHours(stay);
    }
    /**
     * Type of space this vehicle is allowed to use.
     */
    public abstract SpaceType getRequiredSpaceType();

    /**
     * Pricing rule of this kind of vehicle.
     */
    protected abstract PricingPolicy getPricingPolicy();

    /**
     * Normalizes a license plate so "abc 123" and "ABC123" are the same plate.
     * It is a pure helper (no state), which is why it can be static.
     */
    public static String normalizePlate(String licensePlate) {
        String text = requireText(licensePlate, "License plate");
        return text.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be empty.");
        }
        return value.trim();
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getColor() {
        return color;
    }

    public VehicleType getType() {
        return type;
    }

    /**
     * Two vehicles are the same vehicle when they have the same plate.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vehicle vehicle)) {
            return false;
        }
        return licensePlate.equals(vehicle.licensePlate);
    }

    @Override
    public int hashCode() {
        return licensePlate.hashCode();
    }

    @Override
    public String toString() {
        return type + " " + licensePlate + " (" + brand + " " + model + ", " + color + ")";
    }
}