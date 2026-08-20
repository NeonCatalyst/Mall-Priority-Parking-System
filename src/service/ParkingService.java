package service;

import enums.*;
import model.Vehicle;
import model.parking.ParkingSession;
import model.parking.ParkingSlot;
import model.user.PriorityUser;
import model.user.Staff;
import java.time.*;
import java.util.*;

public class ParkingService {
    private final List<ParkingSlot> slots = new ArrayList<>();
    private final List<ParkingSession> activeSessions = new ArrayList<>();
    private final List<ParkingSession> history = new ArrayList<>();

    public void addParkingSlot(ParkingSlot slot) {
        if (slot == null) throw new IllegalArgumentException("Parking slot is required.");
        if (slots.stream().anyMatch(s -> s.getSlotNumber().equalsIgnoreCase(slot.getSlotNumber())))
            throw new IllegalArgumentException("Slot number already exists.");
        slots.add(slot);
    }

    public Optional<ParkingSlot> findSlot(String number) {
        if (number == null) return Optional.empty();
        return slots.stream().filter(s -> s.getSlotNumber().equalsIgnoreCase(number.trim())).findFirst();
    }

    public ParkingSession checkIn(PriorityUser user, Vehicle vehicle, String slotNumber,
            AccessibilityPreference preference, Duration estimatedDuration) {
        ParkingSlot slot = findSlot(slotNumber)
                .orElseThrow(() -> new IllegalArgumentException("Selected parking slot does not exist."));
        return checkIn(user, vehicle, slot, preference, estimatedDuration);
    }

    public ParkingSession checkIn(PriorityUser user, Vehicle vehicle, ParkingSlot slot,
            AccessibilityPreference preference, Duration estimatedDuration) {
        if (user == null || !user.isEligibleForPriorityParking())
            throw new IllegalStateException("Only verified users can check in.");
        if (vehicle == null || !vehicle.belongsTo(user))
            throw new IllegalArgumentException("Vehicle is not registered to this user.");
        if (!slots.contains(slot))
            throw new IllegalArgumentException("Selected parking slot does not exist.");
        if (activeSessions.stream().anyMatch(s -> s.getVehicle() == vehicle))
            throw new IllegalStateException("This vehicle already has an active parking session.");
        if (slot.getStatus() == SlotStatus.RESERVED)
            throw new IllegalStateException("Selected slot is reserved.");
        if (slot.getStatus() == SlotStatus.OCCUPIED)
            throw new IllegalStateException("Selected slot is occupied.");
        if (preference == null) throw new IllegalArgumentException("Accessibility preference is required.");
        if (estimatedDuration == null || estimatedDuration.isZero() || estimatedDuration.isNegative())
            throw new IllegalArgumentException("Estimated duration must be positive.");

        // Revalidate immediately before changing the slot state.
        if (!slot.isAvailable())
            throw new IllegalStateException("Slot availability changed. Please choose another slot.");
        ParkingSession session = new ParkingSession(UUID.randomUUID().toString(), user, vehicle,
                slot, preference, LocalDateTime.now(), estimatedDuration);
        slot.occupy(session);
        activeSessions.add(session);
        return session;
    }

    public void checkOut(PriorityUser user, ParkingSession session) {
        if (user == null || session == null || session.getUser() != user)
            throw new IllegalArgumentException("Users may only check out their own parking sessions.");
        completeCheckout(session);
    }

    public void checkOut(Staff staff, ParkingSession session) {
        if (staff == null || staff.getRole() != StaffRole.PARKING_PERSONNEL)
            throw new IllegalArgumentException("Parking personnel authorization is required.");
        completeCheckout(session);
    }

    public Optional<ParkingSession> findActiveSessionByVehicle(Vehicle vehicle) {
        return activeSessions.stream().filter(s -> s.getVehicle() == vehicle).findFirst();
    }

    public List<ParkingSession> getActiveSessions() { return List.copyOf(activeSessions); }
    public List<ParkingSlot> getParkingSlots() { return List.copyOf(slots); }

    public List<ParkingSession> getParkingHistory(PriorityUser user) {
        if (user == null || !user.isEligibleForPriorityParking())
            throw new IllegalStateException("A verified account is required.");
        return history.stream().filter(s -> s.getUser() == user).toList();
    }

    public List<ParkingSession> getAllParkingHistory(Staff staff) {
        if (staff == null || (staff.getRole() != StaffRole.PARKING_PERSONNEL
                && staff.getRole() != StaffRole.MALL_MANAGEMENT))
            throw new IllegalArgumentException("Authorized staff access is required.");
        return List.copyOf(history);
    }

    public void loadCompletedSession(ParkingSession session, Staff staff) {
        if (staff == null || staff.getRole() != StaffRole.MALL_MANAGEMENT)
            throw new IllegalArgumentException("Mall management authorization is required.");
        if (session == null || session.getStatus() != SessionStatus.COMPLETED)
            throw new IllegalArgumentException("Only completed sessions can be loaded into history.");
        if (history.stream().anyMatch(existing ->
                existing.getSessionId().equals(session.getSessionId())))
            throw new IllegalArgumentException("Parking session already exists in history.");
        history.add(session);
    }

    private void completeCheckout(ParkingSession session) {
        if (session == null || !session.isActive() || !activeSessions.contains(session))
            throw new IllegalStateException("Parking session is not active.");
        session.complete(LocalDateTime.now());
        session.getParkingSlot().release();
        activeSessions.remove(session);
        history.add(session);
    }
}
