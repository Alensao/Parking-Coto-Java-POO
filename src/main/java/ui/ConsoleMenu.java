package ui;

import demo.ParkingDemo;
import enums.PaymentType;
import enums.SpaceStatus;
import enums.SpaceType;
import enums.VehicleType;
import exception.ParkingException;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import model.Car;
import model.CargoVehicle;
import model.Motorcycle;
import model.ParkingSpace;
import model.ParkingTicket;
import model.Payment;
import model.Vehicle;
import service.ParkingLot;

/**
 * Interactive console menu. The user types all the data (vehicles, spaces,
 * times, payment types) and the menu passes it to ParkingLot.
 * It contains NO business rules: every validation and calculation is done
 * by the model classes, and their exceptions are shown as messages.
 */
public class ConsoleMenu {

    /**
     * Creates a vehicle from the data typed by the user.
     * Each vehicle type is linked to its constructor (Car::new, ...),
     * so no switch over the vehicle type is needed.
     */
    @FunctionalInterface
    private interface VehicleCreator {
        Vehicle create(String licensePlate, String brand, String model, String color);
    }

    private final ParkingLot parkingLot;
    private final ConsoleInput input;
    private final Map<VehicleType, VehicleCreator> vehicleCreators;

    public ConsoleMenu(ParkingLot parkingLot, ConsoleInput input) {
        this.parkingLot = parkingLot;
        this.input = input;
        this.vehicleCreators = new EnumMap<>(VehicleType.class);
        this.vehicleCreators.put(VehicleType.MOTORCYCLE, Motorcycle::new);
        this.vehicleCreators.put(VehicleType.CAR, Car::new);
        this.vehicleCreators.put(VehicleType.CARGO, CargoVehicle::new);
    }

    public void start() {
        System.out.println("==============================================");
        System.out.println("  PARKING COTO - " + parkingLot.getName());
        System.out.println("==============================================");
        boolean running = true;
        while (running) {
            printMenu();
            try {
                int option = input.readInt("Choose an option: ", 0, 14);
                System.out.println();
                running = execute(option);
            } catch (ConsoleInput.InputCancelledException exception) {
                System.out.println("You are already in the main menu.");
            }
            System.out.println();
        }
        System.out.println("Goodbye.");
    }

    private void printMenu() {
        System.out.println("---------------- MENU ----------------");
        System.out.println(" 1. Register vehicle");
        System.out.println(" 2. Register parking space");
        System.out.println(" 3. Mark space out of service");
        System.out.println(" 4. Return space to service");
        System.out.println(" 5. Register entry (automatic space)");
        System.out.println(" 6. Register entry (choose space)");
        System.out.println(" 7. Register exit");
        System.out.println(" 8. Register payment");
        System.out.println(" 9. Show available spaces");
        System.out.println("10. Show vehicles inside");
        System.out.println("11. Show active tickets");
        System.out.println("12. Show occupancy by space type");
        System.out.println("13. Show payments and total revenue");
        System.out.println("14. Run automatic demo (17 scenarios)");
        System.out.println(" 0. Exit");
        System.out.println("(Type " + ConsoleInput.CANCEL_KEY
                + " at any question to cancel and return to this menu)");
    }

    /**
     * Runs the chosen option. Business rule violations are caught here and
     * shown to the user, so a mistake never closes the program.
     *
     * @return false when the user chooses to exit
     */
    private boolean execute(int option) {
        try {
            switch (option) {
                case 1 -> registerVehicle();
                case 2 -> registerSpace();
                case 3 -> markSpaceOutOfService();
                case 4 -> returnSpaceToService();
                case 5 -> registerAutomaticEntry();
                case 6 -> registerEntryInSpace();
                case 7 -> registerExit();
                case 8 -> registerPayment();
                case 9 -> showAvailableSpaces();
                case 10 -> showVehiclesInside();
                case 11 -> showActiveTickets();
                case 12 -> showOccupancy();
                case 13 -> showRevenue();
                case 14 -> new ParkingDemo().run();
                default -> {
                    return false;
                }
            }
        } catch (ConsoleInput.InputCancelledException exception) {
            System.out.println("Operation cancelled. Back to the main menu.");
        } catch (ParkingException | IllegalArgumentException exception) {
            System.out.println("ERROR: " + exception.getMessage());
        }
        return true;
    }

    private void registerVehicle() {
        VehicleType type = input.readOption("Vehicle type:", VehicleType.values());
        String plate = input.readText("License plate: ");
        String brand = input.readText("Brand: ");
        String model = input.readText("Model: ");
        String color = input.readText("Color: ");
        Vehicle vehicle = vehicleCreators.get(type).create(plate, brand, model, color);
        parkingLot.registerVehicle(vehicle);
        System.out.println("Registered: " + vehicle);
    }

    private void registerSpace() {
        String id = input.readText("Space id (e.g. A1): ");
        SpaceType type = input.readOption("Space type:", SpaceType.values());
        ParkingSpace space = parkingLot.registerSpace(id, type);
        System.out.println("Registered: " + space);
    }

    private void markSpaceOutOfService() {
        String id = input.readText("Space id: ");
        parkingLot.markSpaceOutOfService(id);
        System.out.println("Space " + id + " is now out of service.");
    }

    private void returnSpaceToService() {
        String id = input.readText("Space id: ");
        parkingLot.returnSpaceToService(id);
        System.out.println("Space " + id + " is available again.");
    }

    private void registerAutomaticEntry() {
        String plate = input.readText("License plate: ");
        LocalDateTime entryTime = input.readDateTime("Entry time");
        ParkingTicket ticket = parkingLot.registerEntry(plate, entryTime);
        printEntry(ticket);
    }

    private void registerEntryInSpace() {
        String plate = input.readText("License plate: ");
        String spaceId = input.readText("Space id: ");
        LocalDateTime entryTime = input.readDateTime("Entry time");
        ParkingTicket ticket = parkingLot.registerEntry(plate, spaceId, entryTime);
        printEntry(ticket);
    }

    private void printEntry(ParkingTicket ticket) {
        System.out.println("Ticket #" + ticket.getTicketNumber() + " generated for "
                + ticket.getVehicle().getLicensePlate() + " in space " + ticket.getSpace().getId()
                + " at " + input.format(ticket.getEntryTime()));
    }

    private void registerExit() {
        String plate = input.readText("License plate: ");
        LocalDateTime exitTime = input.readDateTime("Exit time");
        ParkingTicket ticket = parkingLot.registerExit(plate, exitTime);
        System.out.println("Ticket #" + ticket.getTicketNumber() + " closed.");
        System.out.println("  Stay:          " + ticket.getStayDuration().toMinutes() + " minutes");
        System.out.println("  Charged hours: " + ticket.getChargedHours());
        System.out.println("  Amount to pay: CRC " + ticket.getFinalAmount());
        System.out.println("  Space " + ticket.getSpace().getId() + " released.");
    }

    private void registerPayment() {
        int ticketNumber = input.readInt("Ticket number: ", 1, Integer.MAX_VALUE);
        PaymentType type = input.readOption("Payment type:", PaymentType.values());
        LocalDateTime paymentTime = input.readDateTime("Payment time");
        Payment payment = parkingLot.registerPayment(ticketNumber, type, paymentTime);
        System.out.println("Registered: " + payment);
    }

    private void showAvailableSpaces() {
        printList("Available spaces", parkingLot.getAvailableSpaces());
    }

    private void showVehiclesInside() {
        printList("Vehicles inside", parkingLot.getVehiclesInside());
    }

    private void showActiveTickets() {
        printList("Active tickets", parkingLot.getActiveTickets());
    }

    private void showOccupancy() {
        Map<SpaceType, Long> total = parkingLot.countSpacesByType();
        Map<SpaceType, Long> occupied = parkingLot.countOccupiedSpacesByType();
        System.out.println("Occupancy by space type:");
        for (SpaceType type : SpaceType.values()) {
            System.out.println("  " + type + ": " + occupied.getOrDefault(type, 0L)
                    + " / " + total.getOrDefault(type, 0L));
        }
        System.out.println("  Out of service: " + parkingLot.countSpacesByStatus(SpaceStatus.OUT_OF_SERVICE));
        System.out.printf("  Total occupancy: %.1f%%%n", parkingLot.getOccupancyPercentage());
    }

    private void showRevenue() {
        printList("Payments", parkingLot.getPayments());
        System.out.println("Total revenue: CRC " + parkingLot.getTotalRevenue());
    }

    private void printList(String title, List<?> items) {
        System.out.println(title + ":");
        if (items.isEmpty()) {
            System.out.println("  (none)");
        }
        for (Object item : items) {
            System.out.println("  " + item);
        }
    }
}