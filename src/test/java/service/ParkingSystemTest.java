package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import enums.PaymentType;
import enums.SpaceStatus;
import enums.SpaceType;
import enums.TicketStatus;
import exception.ActiveTicketException;
import exception.IncompatibleSpaceException;
import exception.InvalidTicketStateException;
import exception.NoActiveTicketException;
import exception.SpaceNotAvailableException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import model.Car;
import model.CargoVehicle;
import model.Motorcycle;
import model.ParkingTicket;
import model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pricing.HourlyPricingPolicy;
import pricing.PricingPolicy;

/**
 * Tests for the mandatory cases of the project (section 12) plus
 * boundary cases of the pricing policy and the extra business rules.
 */
class ParkingSystemTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 28, 8, 0);

    private ParkingLot lot;

    @BeforeEach
    void setUp() {
        lot = new ParkingLot("Parking Coto");
        lot.registerSpace("M1", SpaceType.MOTORCYCLE);
        lot.registerSpace("M2", SpaceType.MOTORCYCLE);
        lot.registerSpace("A1", SpaceType.CAR);
        lot.registerSpace("A2", SpaceType.CAR);
        lot.registerSpace("C1", SpaceType.CARGO);

        lot.registerVehicle(new Car("CAR-1", "Toyota", "Corolla", "White"));
        lot.registerVehicle(new Car("CAR-2", "Hyundai", "Accent", "Gray"));
        lot.registerVehicle(new Motorcycle("MOT-1", "Honda", "CB190", "Red"));
        lot.registerVehicle(new CargoVehicle("CRG-1", "Isuzu", "NPR", "Blue"));
    }

    // ---------------------------------------------------------------
    // Entries (cases 1-3)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("1. Car entry: compatible space assigned, ticket ACTIVE")
    void carEntry() {
        ParkingTicket ticket = lot.registerEntry("CAR-1", START);
        assertEquals("A1", ticket.getSpace().getId());
        assertEquals(TicketStatus.ACTIVE, ticket.getStatus());
        assertEquals(SpaceStatus.OCCUPIED, ticket.getSpace().getStatus());
    }

    @Test
    @DisplayName("2. Motorcycle entry")
    void motorcycleEntry() {
        ParkingTicket ticket = lot.registerEntry("MOT-1", START);
        assertEquals("M1", ticket.getSpace().getId());
        assertEquals(SpaceType.MOTORCYCLE, ticket.getSpace().getType());
    }

    @Test
    @DisplayName("3. Cargo vehicle entry")
    void cargoEntry() {
        ParkingTicket ticket = lot.registerEntry("CRG-1", START);
        assertEquals("C1", ticket.getSpace().getId());
        assertEquals(SpaceType.CARGO, ticket.getSpace().getType());
    }

    @Test
    @DisplayName("Automatic assignment skips occupied spaces")
    void automaticAssignmentSkipsOccupied() {
        lot.registerEntry("CAR-1", START);
        ParkingTicket second = lot.registerEntry("CAR-2", START);
        assertEquals("A2", second.getSpace().getId());
    }

    // ---------------------------------------------------------------
    // Invalid entries (cases 4-7)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("4. Assigning an occupied space fails")
    void occupiedSpace() {
        lot.registerEntry("CAR-1", "A1", START);
        assertThrows(SpaceNotAvailableException.class,
                () -> lot.registerEntry("CAR-2", "A1", START));
        assertEquals(1, lot.getActiveTickets().size());
    }

    @Test
    @DisplayName("5. Assigning an out-of-service space fails")
    void outOfServiceSpace() {
        lot.markSpaceOutOfService("A1");
        assertThrows(SpaceNotAvailableException.class,
                () -> lot.registerEntry("CAR-1", "A1", START));
        assertTrue(lot.getActiveTickets().isEmpty());
    }

    @Test
    @DisplayName("6. Assigning an incompatible space fails")
    void incompatibleSpace() {
        assertThrows(IncompatibleSpaceException.class,
                () -> lot.registerEntry("CAR-1", "M1", START));
        assertThrows(IncompatibleSpaceException.class,
                () -> lot.registerEntry("MOT-1", "C1", START));
    }

    @Test
    @DisplayName("7. A vehicle with an active ticket cannot enter again")
    void duplicateActiveTicket() {
        lot.registerEntry("CAR-1", START);
        assertThrows(ActiveTicketException.class, () -> lot.registerEntry("CAR-1", START));
        assertEquals(1, lot.getActiveTickets().size());
    }

    @Test
    @DisplayName("No compatible space free: automatic assignment fails")
    void noSpaceLeft() {
        lot.registerEntry("MOT-1", START);
        lot.registerEntry("CRG-1", START);
        lot.registerVehicle(new CargoVehicle("CRG-2", "Hino", "300", "White"));
        assertThrows(SpaceNotAvailableException.class, () -> lot.registerEntry("CRG-2", START));
    }

    // ---------------------------------------------------------------
    // Stay lengths (cases 8-11), through the real flow
    // ---------------------------------------------------------------

    private long carFeeAfter(Duration stay) {
        lot.registerEntry("CAR-1", START);
        return lot.registerExit("CAR-1", START.plus(stay)).getFinalAmount();
    }

    @Test
    @DisplayName("8. 1 minute is charged as 1 hour")
    void oneMinute() {
        assertEquals(900L, carFeeAfter(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("9. 60 minutes is 1 hour")
    void sixtyMinutes() {
        assertEquals(900L, carFeeAfter(Duration.ofMinutes(60)));
    }

    @Test
    @DisplayName("10. 61 minutes is 2 hours")
    void sixtyOneMinutes() {
        assertEquals(1800L, carFeeAfter(Duration.ofMinutes(61)));
    }

    @Test
    @DisplayName("Charged hours are calculated for the stay")
    void chargedHours() {
        lot.registerEntry("CAR-1", START);
        ParkingTicket ticket = lot.registerExit("CAR-1", START.plusMinutes(61));
        assertEquals(2L, ticket.getChargedHours());
        assertEquals(1800L, ticket.getFinalAmount());
        PricingPolicy policy = new HourlyPricingPolicy(900, 7000);
        assertEquals(3L, policy.calculateChargeableHours(Duration.ofMinutes(121)));
    }

    @Test
    @DisplayName("11. More than 10 hours applies the daily maximum for each vehicle type")
    void dailyMaximum() {
        lot.registerEntry("CAR-1", START);
        lot.registerEntry("MOT-1", START);
        lot.registerEntry("CRG-1", START);
        assertEquals(7000L, lot.registerExit("CAR-1", START.plusHours(11)).getFinalAmount());
        assertEquals(4000L, lot.registerExit("MOT-1", START.plusHours(11)).getFinalAmount());
        assertEquals(11000L, lot.registerExit("CRG-1", START.plusHours(11)).getFinalAmount());
    }

    // ---------------------------------------------------------------
    // Pricing policy boundaries (direct, no parking lot needed)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Table of the statement: 35 min, 1h10, 2h01, 3h exact")
    void statementExamples() {
        PricingPolicy policy = new HourlyPricingPolicy(900, 7000);
        assertEquals(900L, policy.calculateFee(Duration.ofMinutes(35)));
        assertEquals(1800L, policy.calculateFee(Duration.ofMinutes(70)));
        assertEquals(2700L, policy.calculateFee(Duration.ofMinutes(121)));
        assertEquals(2700L, policy.calculateFee(Duration.ofHours(3)));
    }

    @Test
    @DisplayName("Threshold: 9 hours is hourly, 10 hours is the daily maximum")
    void thresholdBoundary() {
        PricingPolicy policy = new HourlyPricingPolicy(900, 7000);
        assertEquals(8100L, policy.calculateFee(Duration.ofHours(9)));
        assertEquals(7000L, policy.calculateFee(Duration.ofHours(10)));
        assertEquals(7000L, policy.calculateFee(Duration.ofHours(10).plusMinutes(1)));
    }

    @Test
    @DisplayName("Each 24-hour period is charged at the daily maximum")
    void multipleDays() {
        PricingPolicy policy = new HourlyPricingPolicy(900, 7000);
        assertEquals(7000L, policy.calculateFee(Duration.ofHours(24)));
        assertEquals(7900L, policy.calculateFee(Duration.ofHours(25)));
        assertEquals(14000L, policy.calculateFee(Duration.ofHours(48)));
    }

    @Test
    @DisplayName("Each vehicle uses its own rate (polymorphism)")
    void ratesPerType() {
        Duration twoHours = Duration.ofHours(2);
        assertEquals(1000L, new Motorcycle("M-9", "Yamaha", "FZ", "Black").calculateFee(twoHours));
        assertEquals(1800L, new Car("C-9", "Kia", "Rio", "Red").calculateFee(twoHours));
        assertEquals(3000L, new CargoVehicle("K-9", "Hino", "500", "White").calculateFee(twoHours));
    }

    @Test
    @DisplayName("A negative stay is rejected")
    void negativeStay() {
        PricingPolicy policy = new HourlyPricingPolicy(900, 7000);
        assertThrows(IllegalArgumentException.class, () -> policy.calculateFee(Duration.ofMinutes(-1)));
    }

    // ---------------------------------------------------------------
    // Close, pay, release, revenue (cases 12-15)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("12. Ticket is closed correctly")
    void closeTicket() {
        lot.registerEntry("CAR-1", START);
        ParkingTicket ticket = lot.registerExit("CAR-1", START.plusMinutes(90));
        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
        assertEquals(1800L, ticket.getFinalAmount());
        assertEquals(START.plusMinutes(90), ticket.getExitTime().orElseThrow());
        assertTrue(lot.getActiveTickets().isEmpty());
    }

    @Test
    @DisplayName("13. Payment is registered correctly")
    void payTicket() {
        lot.registerEntry("CAR-1", START);
        ParkingTicket ticket = lot.registerExit("CAR-1", START.plusMinutes(90));
        Payment payment = lot.registerPayment(ticket.getTicketNumber(),
                PaymentType.SINPE_MOVIL, START.plusMinutes(95));
        assertEquals(1800L, payment.getAmount());
        assertEquals(PaymentType.SINPE_MOVIL, payment.getType());
        assertEquals(TicketStatus.PAID, ticket.getStatus());
        assertEquals(1, lot.getPayments().size());
    }

    @Test
    @DisplayName("14. Space is released when the exit is registered")
    void spaceReleased() {
        ParkingTicket ticket = lot.registerEntry("CAR-1", START);
        assertEquals(SpaceStatus.OCCUPIED, ticket.getSpace().getStatus());
        lot.registerExit("CAR-1", START.plusHours(1));
        assertEquals(SpaceStatus.AVAILABLE, ticket.getSpace().getStatus());
        // and it can be assigned again
        assertEquals("A1", lot.registerEntry("CAR-2", START.plusHours(2)).getSpace().getId());
    }

    @Test
    @DisplayName("15. Total revenue is the sum of the payments")
    void totalRevenue() {
        lot.registerEntry("CAR-1", START);
        lot.registerEntry("CRG-1", START);
        lot.registerEntry("MOT-1", START);
        ParkingTicket car = lot.registerExit("CAR-1", START.plusMinutes(121));   // 2700
        ParkingTicket cargo = lot.registerExit("CRG-1", START.plusHours(11));    // 11000
        ParkingTicket moto = lot.registerExit("MOT-1", START.plusMinutes(35));   // 500

        assertEquals(0L, lot.getTotalRevenue(), "closed but unpaid tickets are not revenue yet");

        lot.registerPayment(car.getTicketNumber(), PaymentType.CASH, START.plusHours(3));
        lot.registerPayment(cargo.getTicketNumber(), PaymentType.CARD, START.plusHours(12));
        assertEquals(13700L, lot.getTotalRevenue());

        lot.registerPayment(moto.getTicketNumber(), PaymentType.CASH, START.plusHours(1));
        assertEquals(14200L, lot.getTotalRevenue());
    }

    // ---------------------------------------------------------------
    // Extra business rules
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Exit without an active ticket fails")
    void exitWithoutTicket() {
        assertThrows(NoActiveTicketException.class, () -> lot.registerExit("CAR-1", START));
    }

    @Test
    @DisplayName("Paying an active ticket fails")
    void payActiveTicket() {
        ParkingTicket ticket = lot.registerEntry("CAR-1", START);
        assertThrows(InvalidTicketStateException.class, () -> lot.registerPayment(
                ticket.getTicketNumber(), PaymentType.CASH, START.plusHours(1)));
    }

    @Test
    @DisplayName("Paying a ticket twice fails")
    void payTwice() {
        lot.registerEntry("CAR-1", START);
        ParkingTicket ticket = lot.registerExit("CAR-1", START.plusHours(1));
        lot.registerPayment(ticket.getTicketNumber(), PaymentType.CASH, START.plusHours(1));
        assertThrows(InvalidTicketStateException.class, () -> lot.registerPayment(
                ticket.getTicketNumber(), PaymentType.CASH, START.plusHours(2)));
        assertEquals(1, lot.getPayments().size());
    }

    @Test
    @DisplayName("Exit time before entry time is rejected and the ticket stays ACTIVE")
    void exitBeforeEntry() {
        ParkingTicket ticket = lot.registerEntry("CAR-1", START);
        assertThrows(IllegalArgumentException.class,
                () -> lot.registerExit("CAR-1", START.minusMinutes(1)));
        assertEquals(TicketStatus.ACTIVE, ticket.getStatus());
        assertEquals(SpaceStatus.OCCUPIED, ticket.getSpace().getStatus());
    }

    @Test
    @DisplayName("Vehicle can enter again after its ticket is closed")
    void reenterAfterExit() {
        lot.registerEntry("CAR-1", START);
        lot.registerExit("CAR-1", START.plusHours(1));
        assertEquals(TicketStatus.ACTIVE,
                lot.registerEntry("CAR-1", START.plusHours(2)).getStatus());
    }

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Available spaces, vehicles inside and active tickets")
    void queries() {
        assertEquals(5, lot.getAvailableSpaces().size());
        lot.registerEntry("CAR-1", START);
        lot.registerEntry("MOT-1", START);
        assertEquals(3, lot.getAvailableSpaces().size());
        assertEquals(2, lot.getVehiclesInside().size());
        assertEquals(2, lot.getActiveTickets().size());
        lot.markSpaceOutOfService("A2");
        assertEquals(2, lot.getAvailableSpaces().size());
        assertFalse(lot.getAvailableSpaces().contains(lot.getSpaces().get(3)));
    }

    @Test
    @DisplayName("Occupancy by space type")
    void occupancyByType() {
        lot.registerEntry("CAR-1", START);
        lot.registerEntry("MOT-1", START);

        Map<SpaceType, Long> total = lot.countSpacesByType();
        Map<SpaceType, Long> occupied = lot.countOccupiedSpacesByType();

        assertEquals(2L, total.get(SpaceType.MOTORCYCLE));
        assertEquals(2L, total.get(SpaceType.CAR));
        assertEquals(1L, total.get(SpaceType.CARGO));
        assertEquals(1L, occupied.get(SpaceType.MOTORCYCLE));
        assertEquals(1L, occupied.get(SpaceType.CAR));
        assertEquals(0L, occupied.get(SpaceType.CARGO));
    }
}