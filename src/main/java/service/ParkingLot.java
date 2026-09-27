package service;

import enums.PaymentType;
import enums.SpaceStatus;
import enums.SpaceType;
import exception.ActiveTicketException;
import exception.NoActiveTicketException;
import exception.ParkingException;
import exception.SpaceNotAvailableException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import model.ParkingSpace;
import model.ParkingTicket;
import model.Payment;
import model.Vehicle;

/**
 * Coordinates the parking lot operations.
 * It keeps the collections, generates ticket and payment numbers and applies
 * the rules that involve the whole parking lot (one active ticket per vehicle,
 * no exit without an active ticket, unique plates and space ids).
 * It does NOT calculate fees (Vehicle -> PricingPolicy), does NOT decide whether
 * a space can be occupied (ParkingSpace) and does NOT decide whether a ticket
 * can be paid (ParkingTicket).
 */
public class ParkingLot {

    private final String name;
    private final Map<String, Vehicle> vehicles;
    private final Map<String, ParkingSpace> spaces;
    private final List<ParkingTicket> tickets;
    private final List<Payment> payments;
    private int nextTicketNumber;
    private int nextPaymentId;

    public ParkingLot(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Parking lot name must not be empty.");
        }
        this.name = name.trim();
        this.vehicles = new HashMap<>();
        this.spaces = new LinkedHashMap<>();
        this.tickets = new ArrayList<>();
        this.payments = new ArrayList<>();
        this.nextTicketNumber = 1;
        this.nextPaymentId = 1;
    }

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    public void registerVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle must not be null.");
        String plate = vehicle.getLicensePlate();
        if (vehicles.containsKey(plate)) {
            throw new ParkingException("A vehicle with plate " + plate + " is already registered.");
        }
        vehicles.put(plate, vehicle);
    }

    public ParkingSpace registerSpace(String id, SpaceType type) {
        ParkingSpace space = new ParkingSpace(id, type);
        if (spaces.containsKey(space.getId())) {
            throw new ParkingException("A space with id " + space.getId() + " is already registered.");
        }
        spaces.put(space.getId(), space);
        return space;
    }

    public void markSpaceOutOfService(String spaceId) {
        findSpace(spaceId).markOutOfService();
    }

    public void returnSpaceToService(String spaceId) {
        findSpace(spaceId).returnToService();
    }

    // ------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------

    /**
     * Registers the entry of a vehicle and assigns the first compatible
     * available space automatically.
     */
    public ParkingTicket registerEntry(String licensePlate, LocalDateTime entryTime) {
        Vehicle vehicle = findVehicle(licensePlate);
        ensureNoActiveTicket(vehicle);
        ParkingSpace space = findFirstAvailableSpaceFor(vehicle);
        return openTicket(vehicle, space, entryTime);
    }

    /**
     * Registers the entry of a vehicle into a specific space.
     * The space itself validates whether it can be occupied.
     */
    public ParkingTicket registerEntry(String licensePlate, String spaceId, LocalDateTime entryTime) {
        Vehicle vehicle = findVehicle(licensePlate);
        ensureNoActiveTicket(vehicle);
        ParkingSpace space = findSpace(spaceId);
        return openTicket(vehicle, space, entryTime);
    }

    /**
     * Registers the exit of a vehicle: closes its active ticket, which
     * calculates the amount and releases the space.
     */
    public ParkingTicket registerExit(String licensePlate, LocalDateTime exitTime) {
        String plate = Vehicle.normalizePlate(licensePlate);
        ParkingTicket ticket = findActiveTicket(plate)
                .orElseThrow(() -> new NoActiveTicketException(
                        "Vehicle " + plate + " has no active ticket."));
        ticket.close(exitTime);
        return ticket;
    }

    /**
     * Registers the payment of a closed ticket.
     */
    public Payment registerPayment(int ticketNumber, PaymentType type, LocalDateTime paymentTime) {
        ParkingTicket ticket = findTicket(ticketNumber);
        Payment payment = ticket.pay(nextPaymentId, type, paymentTime);
        nextPaymentId++;
        payments.add(payment);
        return payment;
    }

    // ------------------------------------------------------------------
    // Queries (they return unmodifiable lists)
    // ------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public List<Vehicle> getVehicles() {
        return List.copyOf(vehicles.values());
    }

    public List<ParkingSpace> getSpaces() {
        return List.copyOf(spaces.values());
    }

    public List<ParkingSpace> getAvailableSpaces() {
        return spaces.values().stream()
                .filter(ParkingSpace::isAvailable)
                .toList();
    }

    public List<ParkingTicket> getTickets() {
        return List.copyOf(tickets);
    }

    public List<ParkingTicket> getActiveTickets() {
        return tickets.stream()
                .filter(ParkingTicket::isActive)
                .toList();
    }

    public List<Vehicle> getVehiclesInside() {
        return getActiveTickets().stream()
                .map(ParkingTicket::getVehicle)
                .toList();
    }

    public List<Payment> getPayments() {
        return List.copyOf(payments);
    }

    /**
     * Total income: sum of all registered payments.
     */
    public long getTotalRevenue() {
        return payments.stream()
                .mapToLong(Payment::getAmount)
                .sum();
    }

    public long countSpacesByStatus(SpaceStatus status) {
        return spaces.values().stream()
                .filter(space -> space.getStatus() == status)
                .count();
    }

    /**
     * Percentage of occupied spaces over all registered spaces.
     */
    public double getOccupancyPercentage() {
        if (spaces.isEmpty()) {
            return 0.0;
        }
        return countSpacesByStatus(SpaceStatus.OCCUPIED) * 100.0 / spaces.size();
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private Vehicle findVehicle(String licensePlate) {
        String plate = Vehicle.normalizePlate(licensePlate);
        Vehicle vehicle = vehicles.get(plate);
        if (vehicle == null) {
            throw new ParkingException("No vehicle registered with plate " + plate + ".");
        }
        return vehicle;
    }

    private ParkingSpace findSpace(String spaceId) {
        if (spaceId == null || spaceId.isBlank()) {
            throw new IllegalArgumentException("Space id must not be empty.");
        }
        String id = spaceId.trim().toUpperCase(Locale.ROOT);
        ParkingSpace space = spaces.get(id);
        if (space == null) {
            throw new ParkingException("Space " + id + " does not exist.");
        }
        return space;
    }

    private ParkingTicket findTicket(int ticketNumber) {
        return tickets.stream()
                .filter(ticket -> ticket.getTicketNumber() == ticketNumber)
                .findFirst()
                .orElseThrow(() -> new ParkingException("Ticket #" + ticketNumber + " does not exist."));
    }

    private Optional<ParkingTicket> findActiveTicket(String licensePlate) {
        return tickets.stream()
                .filter(ticket -> ticket.isActive() && ticket.belongsTo(licensePlate))
                .findFirst();
    }

    /**
     * Business rule 1: a vehicle cannot have two active tickets.
     */
    private void ensureNoActiveTicket(Vehicle vehicle) {
        findActiveTicket(vehicle.getLicensePlate()).ifPresent(ticket -> {
            throw new ActiveTicketException("Vehicle " + vehicle.getLicensePlate()
                    + " already has active ticket #" + ticket.getTicketNumber() + ".");
        });
    }

    private ParkingSpace findFirstAvailableSpaceFor(Vehicle vehicle) {
        return spaces.values().stream()
                .filter(space -> space.canAccept(vehicle))
                .findFirst()
                .orElseThrow(() -> new SpaceNotAvailableException(
                        "No compatible space available for vehicle " + vehicle.getLicensePlate() + "."));
    }

    /**
     * Common logic for both kinds of entry: occupy the space and create the ticket.
     */
    private ParkingTicket openTicket(Vehicle vehicle, ParkingSpace space, LocalDateTime entryTime) {
        Objects.requireNonNull(entryTime, "Entry time must not be null.");
        space.occupy(vehicle);
        ParkingTicket ticket = new ParkingTicket(nextTicketNumber, vehicle, space, entryTime);
        nextTicketNumber++;
        tickets.add(ticket);
        return ticket;
    }

    @Override
    public String toString() {
        return name + ": " + spaces.size() + " spaces, "
                + getActiveTickets().size() + " vehicles inside, revenue CRC " + getTotalRevenue();
    }
}