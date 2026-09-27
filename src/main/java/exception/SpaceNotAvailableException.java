package exception;

/**
 * Thrown when a space is occupied, out of service,
 * or when no compatible space is free.
 */
public class SpaceNotAvailableException extends ParkingException {

    public SpaceNotAvailableException(String message) {
        super(message);
    }
}