package model;

import enums.SpaceStatus;
import enums.SpaceType;
import exception.IncompatibleSpaceException;
import exception.ParkingException;
import exception.SpaceNotAvailableException;
import java.util.Locale;
import java.util.Objects;

/**
 * A single parking space.
 * The space protects its own rules: it cannot be occupied twice, it cannot
 * be used while out of service, and it only accepts compatible vehicles.
 * Its status can only change through methods with a clear intention
 * (occupy, release, markOutOfService, returnToService); there is no setter.
 */
public class ParkingSpace {

    private final String id;
    private final SpaceType type;
    private SpaceStatus status;

    public ParkingSpace(String id, SpaceType type) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Space id must not be empty.");
        }
        this.id = id.trim().toUpperCase(Locale.ROOT);
        this.type = Objects.requireNonNull(type, "Space type must not be null.");
        this.status = SpaceStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == SpaceStatus.AVAILABLE;
    }

    /**
     * A vehicle is compatible when the space type matches the type of space
     * the vehicle requires. The vehicle answers polymorphically; nobody asks
     * whether it is a car, a motorcycle or a cargo vehicle.
     */
    public boolean isCompatibleWith(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle must not be null.");
        return vehicle.getRequiredSpaceType() == type;
    }

    public boolean canAccept(Vehicle vehicle) {
        return isAvailable() && isCompatibleWith(vehicle);
    }

    /**
     * Occupies this space with the given vehicle.
     *
     * @throws SpaceNotAvailableException if the space is out of service or already occupied
     * @throws IncompatibleSpaceException if the vehicle cannot use this type of space
     */
    public void occupy(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle must not be null.");
        if (status == SpaceStatus.OUT_OF_SERVICE) {
            throw new SpaceNotAvailableException("Space " + id + " is out of service.");
        }
        if (status == SpaceStatus.OCCUPIED) {
            throw new SpaceNotAvailableException("Space " + id + " is already occupied.");
        }
        if (!isCompatibleWith(vehicle)) {
            throw new IncompatibleSpaceException("Space " + id + " is for " + type
                    + " and cannot receive vehicle " + vehicle.getLicensePlate() + ".");
        }
        status = SpaceStatus.OCCUPIED;
    }

    /**
     * Frees this space. Only an occupied space can be released.
     */
    public void release() {
        if (status != SpaceStatus.OCCUPIED) {
            throw new ParkingException("Space " + id + " is not occupied, so it cannot be released.");
        }
        status = SpaceStatus.AVAILABLE;
    }

    /**
     * Takes this space out of service. An occupied space cannot be taken
     * out of service because there is a vehicle inside.
     */
    public void markOutOfService() {
        if (status == SpaceStatus.OCCUPIED) {
            throw new ParkingException("Space " + id + " is occupied and cannot be put out of service.");
        }
        status = SpaceStatus.OUT_OF_SERVICE;
    }

    /**
     * Returns an out-of-service space to the available state.
     */
    public void returnToService() {
        if (status != SpaceStatus.OUT_OF_SERVICE) {
            throw new ParkingException("Space " + id + " is not out of service.");
        }
        status = SpaceStatus.AVAILABLE;
    }

    public String getId() {
        return id;
    }

    public SpaceType getType() {
        return type;
    }

    public SpaceStatus getStatus() {
        return status;
    }

    /**
     * Two spaces are the same space when they have the same id.
     */
        @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ParkingSpace)) {
            return false;
        }
        ParkingSpace space = (ParkingSpace) other;
        return id.equals(space.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Space " + id + " [" + type + ", " + status + "]";
    }
}