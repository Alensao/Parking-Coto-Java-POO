package exception;

/**
 * Thrown when an exit is registered for a vehicle without an active ticket.
 */
public class NoActiveTicketException extends ParkingException {

    public NoActiveTicketException(String message) {
        super(message);
    }
}