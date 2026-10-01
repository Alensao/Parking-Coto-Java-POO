package pricing;

import java.time.Duration;

/**
 * Contract for any rule that turns a length of stay into an amount to charge.
 * New pricing rules (night rate, weekend rate, ...) can be added as new
 * implementations without modifying existing classes.
 */
public interface PricingPolicy {

    /**
     * Calculates the fee for a stay.
     *
     * @param stay length of the stay (must not be negative)
     * @return amount to charge, in colones
     */
    long calculateFee(Duration stay);

    /**
     * Calculates how many hours are charged for a stay.
     *
     * @param stay length of the stay (must not be negative)
     * @return number of charged hours
     */
    long calculateChargeableHours(Duration stay);
}