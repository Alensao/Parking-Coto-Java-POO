package demo;

import enums.PaymentType;
import enums.SpaceStatus;
import enums.SpaceType;
import enums.TicketStatus;
import exception.ActiveTicketException;
import exception.IncompatibleSpaceException;
import exception.InvalidTicketStateException;
import exception.NoActiveTicketException;
import exception.SpaceNotAvailableException;
import java.time.LocalDateTime;
import java.util.Objects;
import model.Car;
import model.CargoVehicle;
import model.Motorcycle;
import model.ParkingTicket;
import model.Payment;
import service.ParkingLot;

/**
 * Console demonstration of the parking system.
 * Each scenario prints the expected result, the obtained result and
 * whether they match. It contains no business logic: it only calls
 * ParkingLot and shows what happens.
 */
public class ParkingDemo {

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 28, 8, 0);

    private final ParkingLot parkingLot;
    private int totalScenarios;
    private int passedScenarios;

    public ParkingDemo() {
        this.parkingLot = new ParkingLot("Parking Coto");
    }

    public void run() {
        System.out.println("==============================================");
        System.out.println("  PARKING COTO - Private parking management");
        System.out.println("==============================================\n");

        setUp();
        entryScenarios();
        invalidEntryScenarios();
        feeScenarios();
        exitAndPaymentScenarios();
        extraRuleScenarios();
        printSummary();
    }

    private void setUp() {
        parkingLot.registerSpace("M1", SpaceType.MOTORCYCLE);
        parkingLot.registerSpace("M2", SpaceType.MOTORCYCLE);
        parkingLot.registerSpace("A1", SpaceType.CAR);
        parkingLot.registerSpace("A2", SpaceType.CAR);
        parkingLot.registerSpace("A3", SpaceType.CAR);
        parkingLot.registerSpace("C1", SpaceType.CARGO);

        parkingLot.registerVehicle(new Car("CAR-100", "Toyota", "Corolla", "White"));
        parkingLot.registerVehicle(new Motorcycle("MOT-200", "Honda", "CB190", "Red"));
        parkingLot.registerVehicle(new CargoVehicle("CRG-300", "Isuzu", "NPR", "Blue"));
        parkingLot.registerVehicle(new Car("CAR-400", "Hyundai", "Accent", "Gray"));

        System.out.println("Setup: " + parkingLot.getSpaces().size() + " spaces and "
                + parkingLot.getVehicles().size() + " vehicles registered.\n");
    }

    // 1 - 3
    private void entryScenarios() {
        ParkingTicket carTicket = parkingLot.registerEntry("CAR-100", START);
        check("Car entry is assigned a CAR space", "A1", carTicket.getSpace().getId());

        ParkingTicket motorcycleTicket = parkingLot.registerEntry("MOT-200", START);
        check("Motorcycle entry is assigned a MOTORCYCLE space", "M1", motorcycleTicket.getSpace().getId());

        ParkingTicket cargoTicket = parkingLot.registerEntry("CRG-300", START);
        check("Cargo vehicle entry is assigned a CARGO space", "C1", cargoTicket.getSpace().getId());
    }

    // 4 - 7
    private void invalidEntryScenarios() {
        checkThrows("Assign an occupied space (A1)", SpaceNotAvailableException.class,
                () -> parkingLot.registerEntry("CAR-400", "A1", START));

        parkingLot.markSpaceOutOfService("A2");
        checkThrows("Assign an out-of-service space (A2)", SpaceNotAvailableException.class,
                () -> parkingLot.registerEntry("CAR-400", "A2", START));

        checkThrows("Assign an incompatible space (car into M2)", IncompatibleSpaceException.class,
                () -> parkingLot.registerEntry("CAR-400", "M2", START));

        checkThrows("Enter a vehicle that already has an active ticket", ActiveTicketException.class,
                () -> parkingLot.registerEntry("CAR-100", START));
    }

    // 8 - 11
    private void feeScenarios() {
        check("Car stay of 1 minute (charged as 1 hour)", 900L, stayOfCar400(START, 1));
        check("Car stay of 60 minutes (1 hour)", 900L, stayOfCar400(START.plusHours(2), 60));
        check("Car stay of 61 minutes (2 hours)", 1800L, stayOfCar400(START.plusHours(4), 61));

        ParkingTicket cargoTicket = parkingLot.registerExit("CRG-300", START.plusHours(11));
        check("Cargo stay of 11 hours (daily maximum)", 11000L, cargoTicket.getFinalAmount());
    }

    // 12 - 15
    private void exitAndPaymentScenarios() {
        ParkingTicket carTicket = parkingLot.registerExit("CAR-100", START.plusHours(2).plusMinutes(1));
        check("Close ticket: status after exit", TicketStatus.CLOSED, carTicket.getStatus());

        Payment payment = parkingLot.registerPayment(carTicket.getTicketNumber(),
                PaymentType.SINPE_MOVIL, START.plusHours(2).plusMinutes(5));
        check("Payment of 2 h 1 min stay (3 hours x CRC 900)", 2700L, payment.getAmount());

        check("Space A1 is released after the exit", SpaceStatus.AVAILABLE,
                carTicket.getSpace().getStatus());

        ParkingTicket cargoTicket = parkingLot.getTickets().stream()
                .filter(ticket -> ticket.belongsTo("CRG-300"))
                .findFirst()
                .orElseThrow();
        parkingLot.registerPayment(cargoTicket.getTicketNumber(), PaymentType.CARD, START.plusHours(11));
        check("Total revenue (CRC 2700 + CRC 11000)", 13700L, parkingLot.getTotalRevenue());
    }

    // Extra business rules
    private void extraRuleScenarios() {
        ParkingTicket motorcycleTicket = parkingLot.getActiveTickets().get(0);
        checkThrows("Pay an active ticket", InvalidTicketStateException.class,
                () -> parkingLot.registerPayment(motorcycleTicket.getTicketNumber(),
                        PaymentType.CASH, START.plusHours(1)));

        checkThrows("Register exit without an active ticket", NoActiveTicketException.class,
                () -> parkingLot.registerExit("CAR-100", START.plusHours(3)));
    }

    /**
     * Helper for the fee scenarios: CAR-400 enters, stays some minutes and leaves.
     */
    private long stayOfCar400(LocalDateTime entryTime, int minutes) {
        parkingLot.registerEntry("CAR-400", entryTime);
        ParkingTicket ticket = parkingLot.registerExit("CAR-400", entryTime.plusMinutes(minutes));
        return ticket.getFinalAmount();
    }

    private void check(String description, Object expected, Object obtained) {
        boolean passed = Objects.equals(expected, obtained);
        printResult(description, String.valueOf(expected), String.valueOf(obtained), passed);
    }

    private void checkThrows(String description, Class<? extends RuntimeException> expected, Runnable action) {
        String obtained;
        boolean passed;
        try {
            action.run();
            obtained = "no exception";
            passed = false;
        } catch (RuntimeException exception) {
            obtained = exception.getClass().getSimpleName() + " - " + exception.getMessage();
            passed = expected.isInstance(exception);
        }
        printResult(description, expected.getSimpleName(), obtained, passed);
    }

    private void printResult(String description, String expected, String obtained, boolean passed) {
        totalScenarios++;
        if (passed) {
            passedScenarios++;
        }
        System.out.printf("%2d. %s%n", totalScenarios, description);
        System.out.println("    Expected: " + expected);
        System.out.println("    Obtained: " + obtained);
        System.out.println("    " + (passed ? "[OK]" : "[FAIL]") + "\n");
    }

    private void printSummary() {
        System.out.println("==============================================");
        System.out.println("Scenarios passed: " + passedScenarios + " / " + totalScenarios);
        System.out.println("Vehicles inside:  " + parkingLot.getVehiclesInside());
        System.out.printf("Occupancy:        %.1f%%%n", parkingLot.getOccupancyPercentage());
        System.out.println("Total revenue:    CRC " + parkingLot.getTotalRevenue());
        System.out.println("==============================================");
    }
}
