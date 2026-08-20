package ui;

import enums.*;
import model.Vehicle;
import model.parking.*;
import model.user.PriorityUser;
import service.ParkingService;
import service.ReportService;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ParkingConsole {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private final ParkingService parkingService;
    private final Scanner scanner;
    private ParkingOption waitingSelection;

    public ParkingConsole(ParkingService parkingService, Scanner scanner) {
        this.parkingService = parkingService;
        this.scanner = scanner;
    }

    public AccessibilityPreference askAccessibilityPreference() {
        System.out.println("\nWhat would you like to be closest to?");
        System.out.println("1. Accessible Entrance\n2. Elevator\n3. Ramp\n4. No Preference");
        System.out.print("Choice: ");
        return switch (scanner.nextLine()) {
            case "1" -> AccessibilityPreference.ACCESSIBLE_ENTRANCE;
            case "2" -> AccessibilityPreference.ELEVATOR;
            case "3" -> AccessibilityPreference.RAMP;
            case "4" -> AccessibilityPreference.NO_PREFERENCE;
            default -> throw new IllegalArgumentException("Invalid accessibility preference.");
        };
    }

    public void displayAvailability(AccessibilityPreference preference) {
        System.out.println("\nPRIORITY PARKING");
        System.out.println("Preference: " + preferenceLabel(preference));
        for (ParkingSlot slot : parkingService.getParkingSlots()) {
            int distance = slot.getLocation().getDistanceTo(preference);
            System.out.printf("%s | %-9s | %s | %d m from %s%n",
                    slot.getSlotNumber(), slot.getStatus(), slot.getLocation().getDescription(),
                    distance, preferenceLabel(preference));
        }
    }

    public void displayRecommendations(ParkingRecommendation recommendation) {
        System.out.println("Preference: " + recommendation.getPreference());
        int number = 1;
        for (ParkingOption option : recommendation.getOptions()) {
            System.out.println("\n------------------------------------------------------------");
            System.out.println("[" + number++ + "] " + optionTitle(option.getOptionType()));
            System.out.println("Slot: " + option.getParkingSlot().getSlotNumber());
            System.out.println("Status: [" + option.getDisplayedStatus() + "]");
            System.out.println("Location: " + option.getParkingSlot().getLocation().getDescription());
            System.out.println("Distance: " + option.getDistanceMeters() + " meters from "
                    + preferenceLabel(recommendation.getPreference()));
            if (option.getEstimatedWait().isZero())
                System.out.println("Waiting Time: None");
            else
                System.out.println("Estimated Wait: About "
                        + ((option.getEstimatedWait().toSeconds() + 59) / 60) + " minutes");
            System.out.println("Best for: " + bestFor(option.getOptionType()));
            System.out.println("Reason: " + option.getExplanation());
            if (option.getOptionType() == ParkingOptionType.WAIT_FOR_CLOSER_SLOT)
                System.out.println("Notice: Waiting does not reserve the slot and availability is not guaranteed.");
        }
    }

    public ParkingOption askRecommendationChoice(ParkingRecommendation recommendation) {
        System.out.print("\nSelect option number: ");
        int choice = Integer.parseInt(scanner.nextLine());
        if (choice < 1 || choice > recommendation.getOptions().size())
            throw new IllegalArgumentException("Invalid recommendation selection.");
        ParkingOption selected = recommendation.getOptions().get(choice - 1);
        if (selected.getOptionType() == ParkingOptionType.WAIT_FOR_CLOSER_SLOT) {
            waitingSelection = selected;
            System.out.println("Waiting choice recorded. The slot remains occupied and has not been reserved.");
        }
        return selected;
    }

    public boolean isWaitingSlotAvailable() {
        return waitingSelection != null && waitingSelection.getParkingSlot().isAvailable();
    }

    public ParkingSlot askForAvailableSlot() {
        System.out.print("Enter an available slot number: ");
        ParkingSlot slot = parkingService.findSlot(scanner.nextLine())
                .orElseThrow(() -> new IllegalArgumentException("Selected parking slot does not exist."));
        if (!slot.isAvailable()) throw new IllegalStateException("Selected slot is " + slot.getStatus() + ".");
        return slot;
    }

    public Duration askEstimatedDuration() {
        System.out.println("\nEstimated parking duration:");
        System.out.println("1. 30 minutes\n2. 1 hour\n3. 2 hours\n4. 3 hours\n5. 4 hours\n6. Custom minutes");
        System.out.print("Choice: ");
        return switch (scanner.nextLine()) {
            case "1" -> Duration.ofMinutes(30);
            case "2" -> Duration.ofHours(1);
            case "3" -> Duration.ofHours(2);
            case "4" -> Duration.ofHours(3);
            case "5" -> Duration.ofHours(4);
            case "6" -> {
                System.out.print("Enter positive number of minutes: ");
                long minutes = Long.parseLong(scanner.nextLine());
                if (minutes <= 0) throw new IllegalArgumentException("Duration must be positive.");
                yield Duration.ofMinutes(minutes);
            }
            default -> throw new IllegalArgumentException("Invalid duration selection.");
        };
    }

    public ParkingSession confirmCheckIn(PriorityUser user, Vehicle vehicle, ParkingSlot slot,
            AccessibilityPreference preference, Duration duration) {
        System.out.println("\nCONFIRM PARKING");
        System.out.println("Vehicle: " + vehicle.getPlateNumber());
        System.out.println("Slot: " + slot.getSlotNumber());
        System.out.println("Location: " + slot.getLocation().getDescription());
        System.out.println("Accessibility Preference: " + preferenceLabel(preference));
        System.out.println("Distance: " + slot.getLocation().getDistanceTo(preference) + " meters");
        System.out.println("Estimated Duration: " + formatDuration(duration));
        System.out.print("Confirm check-in? (Y/N): ");
        if (!scanner.nextLine().equalsIgnoreCase("Y")) {
            System.out.println("Check-in cancelled.");
            return null;
        }
        ParkingSession session = parkingService.checkIn(
                user, vehicle, slot.getSlotNumber(), preference, duration);
        System.out.println("\nVEHICLE CHECKED IN");
        displayActiveSession(session);
        return session;
    }

    public void displayActiveSession(ParkingSession session) {
        System.out.println("Vehicle: " + session.getVehicle().getPlateNumber());
        System.out.println("Slot: " + session.getParkingSlot().getSlotNumber());
        System.out.println("Check-In: " + session.getCheckInTime().format(TIME));
        System.out.println("Estimated Duration: " + formatDuration(session.getEstimatedDuration()));
        System.out.println("Estimated Departure: " + session.getEstimatedDepartureTime().format(TIME));
        System.out.println("Preference: " + preferenceLabel(session.getAccessibilityPreference()));
        System.out.println("Location: " + session.getParkingSlot().getLocation().getDescription());
    }

    public void displayHistory(PriorityUser user) {
        List<ParkingSession> history = parkingService.getParkingHistory(user);
        System.out.println("\nPARKING HISTORY FOR " + user.getName().toUpperCase());
        if (history.isEmpty()) {
            System.out.println("No completed parking sessions.");
            return;
        }
        for (int index = 0; index < history.size(); index++) {
            ParkingSession session = history.get(index);
            System.out.println((index + 1) + ". Date: " + session.getCheckInTime().format(DATE));
            System.out.println("   Vehicle: " + session.getVehicle().getPlateNumber());
            System.out.println("   Slot: " + session.getParkingSlot().getSlotNumber());
            System.out.println("   Check-In: " + session.getCheckInTime().format(TIME));
            System.out.println("   Check-Out: " + session.getCheckOutTime().format(TIME));
            System.out.println("   Duration: " + formatDuration(session.getActualDuration()));
        }
    }

    public void displayPersonnelMonitoring() {
        ReportService reports = new ReportService();
        List<ParkingSlot> slots = parkingService.getParkingSlots();
        System.out.println("\nPERSONNEL PARKING MONITOR");
        System.out.println("AVAILABLE: " + reports.countSlotsByStatus(slots, SlotStatus.AVAILABLE));
        System.out.println("OCCUPIED: " + reports.countSlotsByStatus(slots, SlotStatus.OCCUPIED));
        System.out.println("RESERVED: " + reports.countSlotsByStatus(slots, SlotStatus.RESERVED));
        System.out.println("Active Sessions: " + parkingService.getActiveSessions().size());
        for (ParkingSlot slot : slots) {
            String vehicle = slot.getActiveSession() == null ? "" :
                    " | Vehicle: " + slot.getActiveSession().getVehicle().getPlateNumber();
            System.out.println(slot.getSlotNumber() + " | " + slot.getStatus() + vehicle);
        }
    }

    public static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        if (hours == 0) return minutes + " minutes";
        if (minutes == 0) return hours + (hours == 1 ? " hour" : " hours");
        return hours + "h " + minutes + "m";
    }

    private String preferenceLabel(AccessibilityPreference preference) {
        return switch (preference) {
            case ACCESSIBLE_ENTRANCE -> "Accessible Entrance";
            case ELEVATOR -> "Elevator";
            case RAMP -> "Ramp";
            case NO_PREFERENCE -> "Nearest Accessible Facility";
        };
    }

    private String optionTitle(ParkingOptionType type) {
        return switch (type) {
            case PARK_NOW -> "PARK NOW";
            case WAIT_FOR_CLOSER_SLOT -> "WAIT FOR A CLOSER SLOT";
            case ALTERNATIVE -> "ALTERNATIVE";
        };
    }

    private String bestFor(ParkingOptionType type) {
        return switch (type) {
            case PARK_NOW -> "Parking immediately.";
            case WAIT_FOR_CLOSER_SLOT -> "A shorter walking distance if you are willing to wait.";
            case ALTERNATIVE -> "An immediate backup option.";
        };
    }
}
