package testing;

import app.DemoData;
import enums.*;
import model.Vehicle;
import model.parking.*;
import model.user.*;
import recommendation.RuleBasedRecommendationStrategy;
import service.*;
import ui.ConsoleUI;

import java.time.Duration;
import java.util.*;

public class RegressionTest {
    private static int passed;

    public static void main(String[] args) {
        DemoData data = new DemoData();
        data.seed();
        AccountService accounts = data.getAccountService();
        StaffService staffService = data.getStaffService();
        ParkingService parking = data.getParkingService();
        Staff personnel = staffService.authenticate("STAFF-001", "park123");
        Staff manager = staffService.authenticate("ADMIN-001", "admin123");

        check("Correct parking-personnel login",
                personnel.getRole() == StaffRole.PARKING_PERSONNEL);
        check("Correct management login",
                manager.getRole() == StaffRole.MALL_MANAGEMENT);
        expectFailure("Wrong staff password rejected",
                () -> staffService.authenticate("STAFF-001", "wrong-password"));
        expectFailure("Unknown Staff ID rejected",
                () -> staffService.authenticate("UNKNOWN-001", "park123"));

        PriorityUser maria = accounts.findByAccountId("MP-10001").orElseThrow();
        Vehicle mariaCar = maria.getVehicles().get(0);
        expectFailure("Normal user cannot access staff authentication",
                () -> staffService.authenticate(maria.getAccountId(), "1234"));
        ParkingSession existingHistory = parking.getAllParkingHistory(manager).get(0);
        expectFailure("Parking personnel blocked from management-only operation",
                () -> parking.loadCompletedSession(existingHistory, personnel));
        PriorityUser pending = accounts.getPendingAccounts().get(0);

        check("Create account and pending status",
                pending.getVerificationStatus() == VerificationStatus.PENDING);
        expectFailure("Pending restriction", () -> parking.checkIn(
                pending, pending.getVehicles().get(0), "P-01",
                AccessibilityPreference.ELEVATOR, Duration.ofHours(1)));
        accounts.verifyAccount(pending, personnel);
        check("Personnel verification", pending.isEligibleForPriorityParking()
                && pending.getAccountId() != null && pending.getQrToken() != null);

        check("Account ID login",
                accounts.authenticateWithAccountId(maria.getAccountId(), "1234") == maria);
        check("QR-token login",
                accounts.authenticateWithQr(maria.getQrToken(), "1234") == maria);
        expectFailure("Wrong PIN", () ->
                accounts.authenticateWithAccountId(maria.getAccountId(), "9999"));

        Vehicle added = accounts.addVehicle(maria, "NEW 5000", VehicleType.MOTORCYCLE);
        check("Add and select registered vehicle", added.belongsTo(maria)
                && maria.getVehicles().contains(added));

        RecommendationService recommendationService =
                new RecommendationService(new RuleBasedRecommendationStrategy());
        ParkingRecommendation recommendation = recommendationService.getParkingChoices(
                maria, mariaCar, AccessibilityPreference.ELEVATOR,
                parking.getParkingSlots(), parking.getAllParkingHistory(personnel));
        check("AI recommendation generated", !recommendation.getOptions().isEmpty());
        check("PARK_NOW generated", hasType(recommendation, ParkingOptionType.PARK_NOW));
        check("WAIT option generated", hasType(recommendation, ParkingOptionType.WAIT_FOR_CLOSER_SLOT));
        check("ALTERNATIVE generated", hasType(recommendation, ParkingOptionType.ALTERNATIVE));
        check("Refresh recommendation", !recommendationService.getParkingChoices(
                maria, mariaCar, AccessibilityPreference.ELEVATOR,
                parking.getParkingSlots(), parking.getAllParkingHistory(personnel))
                .getOptions().isEmpty());

        ReportService reports = data.getReportService();
        check("Parking map state", reports.countSlotsByStatus(
                parking.getParkingSlots(), SlotStatus.RESERVED) == 1);

        ParkingSlot selected = recommendation.getOptions().stream()
                .filter(option -> option.getOptionType() == ParkingOptionType.PARK_NOW)
                .findFirst().orElseThrow().getParkingSlot();
        ParkingSession session = parking.checkIn(maria, mariaCar, selected.getSlotNumber(),
                AccessibilityPreference.ELEVATOR, Duration.ofHours(2));
        check("Check-in", session.isActive() && selected.getStatus() == SlotStatus.OCCUPIED);
        expectFailure("Duplicate check-in prevention", () -> parking.checkIn(
                maria, mariaCar, "P-01", AccessibilityPreference.ELEVATOR, Duration.ofHours(1)));
        check("Active parking", parking.findActiveSessionByVehicle(mariaCar).isPresent());

        parking.checkOut(maria, session);
        check("Check-out", session.getStatus() == SessionStatus.COMPLETED
                && selected.getStatus() == SlotStatus.AVAILABLE);
        check("User parking history", parking.getParkingHistory(maria).contains(session));
        check("Personnel monitoring", !parking.getActiveSessions().isEmpty());
        check("Management history", parking.getAllParkingHistory(manager).contains(session));
        check("Management report", reports.calculateAverageParkingDuration(
                parking.getAllParkingHistory(manager)) != null);

        ConsoleUI inputTest = new ConsoleUI(new Scanner("wrong\n9\n2\n"));
        check("Invalid menu input recovery", inputTest.readChoice("Test choice: ", 0, 4) == 2);

        System.out.println("\nRegression result: " + passed + " checks passed.");
    }

    private static boolean hasType(ParkingRecommendation recommendation, ParkingOptionType type) {
        return recommendation.getOptions().stream().anyMatch(option -> option.getOptionType() == type);
    }

    private static void check(String name, boolean condition) {
        if (!condition) throw new AssertionError(name + " failed.");
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static void expectFailure(String name, Runnable action) {
        try {
            action.run();
            throw new AssertionError(name + " should have been rejected.");
        } catch (IllegalArgumentException | IllegalStateException expected) {
            passed++;
            System.out.println("[PASS] " + name);
        }
    }
}
