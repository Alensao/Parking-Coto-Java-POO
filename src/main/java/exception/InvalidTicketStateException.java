package exception;

/**
 * Thrown when a ticket operation is not allowed in its current state
 * (for example, paying an active ticket).
 */
public class InvalidTicketStateException extends ParkingException {

    public InvalidTicketStateException(String message) {
        super(message);
    }
}