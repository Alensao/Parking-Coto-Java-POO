package model;

import enums.SpaceType;
import enums.VehicleType;
import pricing.HourlyPricingPolicy;
import pricing.PricingPolicy;

/**
 * A motorcycle.
 * Uses MOTORCYCLE spaces and pays CRC 500 per hour, with a daily maximum of CRC 4000.
 */
public class Motorcycle extends Vehicle {

    private static final PricingPolicy PRICING_POLICY = new HourlyPricingPolicy(500, 4000);

    public Motorcycle(String licensePlate, String brand, String model, String color) {
        super(licensePlate, brand, model, color, VehicleType.MOTORCYCLE);
    }

    @Override
    public SpaceType getRequiredSpaceType() {
        return SpaceType.MOTORCYCLE;
    }

    @Override
    protected PricingPolicy getPricingPolicy() {
        return PRICING_POLICY;
    }
}