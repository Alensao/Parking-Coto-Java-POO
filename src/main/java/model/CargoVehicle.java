package model;

import enums.SpaceType;
import enums.VehicleType;
import pricing.HourlyPricingPolicy;
import pricing.PricingPolicy;

/**
 * A cargo vehicle (trucks, vans).
 * Uses CARGO spaces and pays CRC 1500 per hour, with a daily maximum of CRC 11000.
 */
public class CargoVehicle extends Vehicle {

    private static final PricingPolicy PRICING_POLICY = new HourlyPricingPolicy(1500, 11000);

    public CargoVehicle(String licensePlate, String brand, String model, String color) {
        super(licensePlate, brand, model, color, VehicleType.CARGO);
    }

    @Override
    public SpaceType getRequiredSpaceType() {
        return SpaceType.CARGO;
    }

    @Override
    protected PricingPolicy getPricingPolicy() {
        return PRICING_POLICY;
    }
}