package exception;

/**
 * Thrown when a vehicle tries to use a space of a different type.
 */
public class IncompatibleSpaceException extends ParkingException {

    public IncompatibleSpaceException(String message) {
        super(message);
    }
}