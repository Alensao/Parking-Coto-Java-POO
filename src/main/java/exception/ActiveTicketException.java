package exception;

/**
 * Thrown when a vehicle that already has an active ticket tries to enter again.
 */
public class ActiveTicketException extends ParkingException {

    public ActiveTicketException(String message) {
        super(message);
    }
}