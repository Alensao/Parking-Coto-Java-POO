
package enums;

/**
 * Descriptive type of a vehicle.
 * <p>
 * It is used only to display information. Fees and space compatibility
 * are resolved through polymorphism in the Vehicle subclasses, never by
 * asking for this value.
 */
public enum VehicleType {
    MOTORCYCLE,
    CAR,
    CARGO
}