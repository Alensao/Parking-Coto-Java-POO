package model;

import enums.PaymentType;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable record of a payment made for a closed ticket.
 * The amount is taken from the ticket, so both can never disagree.
 * The constructor is package-private: payments are created only through
 * ParkingTicket.pay(...), which validates the ticket status first.
 */
public class Payment {

    private final int id;
    private final ParkingTicket ticket;
    private final LocalDateTime paymentTime;
    private final long amount;
    private final PaymentType type;

    Payment(int id, ParkingTicket ticket, LocalDateTime paymentTime, PaymentType type) {
        if (id <= 0) {
            throw new IllegalArgumentException("Payment id must be greater than zero.");
        }
        this.id = id;
        this.ticket = Objects.requireNonNull(ticket, "Ticket must not be null.");
        this.paymentTime = Objects.requireNonNull(paymentTime, "Payment time must not be null.");
        this.type = Objects.requireNonNull(type, "Payment type must not be null.");
        this.amount = ticket.getFinalAmount();
    }

    public int getId() {
        return id;
    }

    public ParkingTicket getTicket() {
        return ticket;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public long getAmount() {
        return amount;
    }

    public PaymentType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Payment #" + id + " for ticket #" + ticket.getTicketNumber()
                + ": CRC " + amount + " (" + type + ")";
    }
}
