package model;

import enums.PaymentType;
import enums.TicketStatus;
import exception.InvalidTicketStateException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents one stay of a vehicle in a parking space.
 * The ticket controls its own life cycle: ACTIVE -> CLOSED -> PAID.
 * Closing the ticket calculates the amount (asking the vehicle, polymorphism)
 * and releases the space. Paying the ticket creates the Payment.
 */
public class ParkingTicket {

    private final int ticketNumber;
    private final Vehicle vehicle;
    private final ParkingSpace space;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TicketStatus status;
    private long finalAmount;

    public ParkingTicket(int ticketNumber, Vehicle vehicle, ParkingSpace space, LocalDateTime entryTime) {
        if (ticketNumber <= 0) {
            throw new IllegalArgumentException("Ticket number must be greater than zero.");
        }
        this.ticketNumber = ticketNumber;
        this.vehicle = Objects.requireNonNull(vehicle, "Vehicle must not be null.");
        this.space = Objects.requireNonNull(space, "Space must not be null.");
        this.entryTime = Objects.requireNonNull(entryTime, "Entry time must not be null.");
        this.status = TicketStatus.ACTIVE;
        this.finalAmount = 0;
    }

    public boolean isActive() {
        return status == TicketStatus.ACTIVE;
    }

    /**
     * Tells whether this ticket belongs to the vehicle with the given plate.
     */
    public boolean belongsTo(String licensePlate) {
        return vehicle.getLicensePlate().equals(Vehicle.normalizePlate(licensePlate));
    }

    /**
     * Length of the stay. Only available once the ticket has an exit time.
     */
    public Duration getStayDuration() {
        if (exitTime == null) {
            throw new InvalidTicketStateException("Ticket " + ticketNumber + " has no exit time yet.");
        }
        return Duration.between(entryTime, exitTime);
    }

    /**
     * Registers the exit: calculates the final amount, releases the space
     * and changes the status to CLOSED.
     *
     * @throws InvalidTicketStateException if the ticket is not ACTIVE
     */
    public void close(LocalDateTime exitTime) {
        Objects.requireNonNull(exitTime, "Exit time must not be null.");
        if (status != TicketStatus.ACTIVE) {
            throw new InvalidTicketStateException("Ticket " + ticketNumber
                    + " cannot be closed because it is " + status + ".");
        }
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Exit time cannot be before entry time.");
        }
        long amount = vehicle.calculateFee(Duration.between(entryTime, exitTime));
        space.release();
        this.exitTime = exitTime;
        this.finalAmount = amount;
        this.status = TicketStatus.CLOSED;
    }

    /**
     * Pays this ticket and returns the resulting payment.
     *
     * @throws InvalidTicketStateException if the ticket is ACTIVE or already PAID
     */
    public Payment pay(int paymentId, PaymentType type, LocalDateTime paymentTime) {
        if (status == TicketStatus.ACTIVE) {
            throw new InvalidTicketStateException("Ticket " + ticketNumber
                    + " is still active: register the exit before paying.");
        }
        if (status == TicketStatus.PAID) {
            throw new InvalidTicketStateException("Ticket " + ticketNumber + " is already paid.");
        }
        Objects.requireNonNull(paymentTime, "Payment time must not be null.");
        if (paymentTime.isBefore(exitTime)) {
            throw new IllegalArgumentException("Payment time cannot be before exit time.");
        }
        Payment payment = new Payment(paymentId, this, paymentTime, type);
        this.status = TicketStatus.PAID;
        return payment;
    }

    public int getTicketNumber() {
        return ticketNumber;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpace getSpace() {
        return space;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    /**
     * Exit time, or empty while the ticket is still ACTIVE.
     */
    public Optional<LocalDateTime> getExitTime() {
        return Optional.ofNullable(exitTime);
    }

    public TicketStatus getStatus() {
        return status;
    }

    public long getFinalAmount() {
        return finalAmount;
    }

    /**
     * Two tickets are the same ticket when they have the same number.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ParkingTicket ticket)) {
            return false;
        }
        return ticketNumber == ticket.ticketNumber;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(ticketNumber);
    }

    @Override
    public String toString() {
        return "Ticket #" + ticketNumber + " [" + status + "] " + vehicle.getLicensePlate()
                + " in space " + space.getId() + ", amount CRC " + finalAmount;
    }
}