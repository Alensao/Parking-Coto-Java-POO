package exception;

/**
 * Base exception for every business rule violation in the parking system.
 * It is unchecked because it represents an invalid operation, not an
 * external failure (files, network, etc.).
 */
public class ParkingException extends RuntimeException {

    public ParkingException(String message) {
        super(message);
    }
}