package app;

import enums.*;
import model.Vehicle;
import model.parking.*;
import model.user.*;
import service.*;
import java.time.*;

public class DemoData {
    private final AccountService accounts = new AccountService();
    private final StaffService staffService = new StaffService();
    private final ParkingService parking = new ParkingService();
    private final ReportService reports = new ReportService();
    private Staff personnel;
    private Staff manager;

    public void seed() {
        personnel = staffService.registerStaff(
                "STAFF-001", "Ana Reyes", StaffRole.PARKING_PERSONNEL, "park123");
        manager = staffService.registerStaff(
                "ADMIN-001", "Daniel Lim", StaffRole.MALL_MANAGEMENT, "admin123");
        PriorityUser maria = accounts.registerUser(
                "Maria Santos", UserType.PWD, "PWD-DEMO-001", "1234", "1234");
        Vehicle mariaCar = accounts.registerVehicle(maria, "ABC 1234", VehicleType.CAR);
        accounts.registerVehicle(maria, "XYZ 5678", VehicleType.VAN);
        accounts.verifyAccount(maria, personnel);

        PriorityUser roberto = accounts.registerUser(
                "Roberto Cruz", UserType.SENIOR_CITIZEN, "SC-DEMO-002", "5678", "5678");
        Vehicle robertoCar = accounts.registerVehicle(roberto, "SEN 2026", VehicleType.CAR);
        accounts.verifyAccount(roberto, personnel);

        PriorityUser pending = accounts.registerUser(
                "Lina Gomez", UserType.PWD, "PWD-PENDING-003", "2468", "2468");
        accounts.registerVehicle(pending, "PEN 3000", VehicleType.CAR);

        addSlot("P-01", "Ground Floor", "A", 10, 25, 15);
        addSlot("P-02", "Ground Floor", "A", 15, 5, 20);
        addSlot("P-03", "Ground Floor", "B", 20, 10, 30);
        addSlot("P-04", "Ground Floor", "B", 45, 20, 10);
        addSlot("P-05", "Second Floor", "A", 30, 15, 35);
        parking.findSlot("P-04").orElseThrow().reserve();

        createHistoricalSession(maria, mariaCar);
        parking.checkIn(roberto, robertoCar, "P-02",
                AccessibilityPreference.ELEVATOR, Duration.ofMinutes(3));
    }

    private void createHistoricalSession(PriorityUser user, Vehicle vehicle) {
        ParkingSlot oldSlot = new ParkingSlot("HISTORY-SLOT",
                new ParkingLocation("Mall", "Ground Floor", "History", 20, 20, 20));
        LocalDateTime checkout = LocalDateTime.now().minusDays(1);
        ParkingSession historical = new ParkingSession(
                "DEMO-HISTORY-001", user, vehicle, oldSlot,
                AccessibilityPreference.ELEVATOR,
                checkout.minusMinutes(5), Duration.ofMinutes(5));
        oldSlot.occupy(historical);
        historical.complete(checkout);
        oldSlot.release();
        parking.loadCompletedSession(historical, manager);
    }

    private void addSlot(String number, String floor, String section,
                         int entrance, int elevator, int ramp) {
        parking.addParkingSlot(new ParkingSlot(number,
                new ParkingLocation("Mall", floor, section, entrance, elevator, ramp)));
    }

    public AccountService getAccountService() { return accounts; }
    public StaffService getStaffService() { return staffService; }
    public ParkingService getParkingService() { return parking; }
    public ReportService getReportService() { return reports; }
    public Staff getParkingPersonnel() { return personnel; }
    public Staff getMallManager() { return manager; }
}
