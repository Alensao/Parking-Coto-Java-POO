package model;

import enums.SpaceType;
import enums.VehicleType;
import pricing.HourlyPricingPolicy;
import pricing.PricingPolicy;

/**
 * A passenger car.
 * Uses CAR spaces and pays CRC 900 per hour, with a daily maximum of CRC 7000.
 */
public class Car extends Vehicle {

    private static final PricingPolicy PRICING_POLICY = new HourlyPricingPolicy(900, 7000);

    public Car(String licensePlate, String brand, String model, String color) {
        super(licensePlate, brand, model, color, VehicleType.CAR);
    }

    @Override
    public SpaceType getRequiredSpaceType() {
        return SpaceType.CAR;
    }

    @Override
    protected PricingPolicy getPricingPolicy() {
        return PRICING_POLICY;
    }
}