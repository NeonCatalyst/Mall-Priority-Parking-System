package app;

import enums.*;
import model.Vehicle;
import model.parking.*;
import model.user.*;
import recommendation.RuleBasedRecommendationStrategy;
import service.*;
import ui.*;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MallParkingApplication {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");
    private final AccountService accounts;
    private final StaffService staffService;
    private final ParkingService parking;
    private final ReportService reports;
    private final RecommendationService recommendations =
            new RecommendationService(new RuleBasedRecommendationStrategy());
    private final Staff manager;
    private final ConsoleUI ui;
    private final ParkingConsole parkingConsole;

    public MallParkingApplication(DemoData data, Scanner scanner) {
        accounts = data.getAccountService();
        staffService = data.getStaffService();
        parking = data.getParkingService();
        reports = data.getReportService();
        manager = data.getMallManager();
        ui = new ConsoleUI(scanner);
        parkingConsole = new ParkingConsole(parking, scanner);
    }

    public void run() {
        boolean running = true;
        while (running) {
            ui.printHeader("MALL PRIORITY PARKING SYSTEM");
            System.out.println("             Priority Parking for");
            System.out.println("          PWDs & Senior Citizens\n");
            System.out.println("MAIN MENU\n");
            System.out.println("[1] Create Account");
            System.out.println("[2] User Sign In");
            System.out.println("[3] Staff Sign In");
            System.out.println("[0] Exit\n");
            int choice = ui.readChoice("Enter choice: ", 0, 3);
            try {
                switch (choice) {
                    case 1 -> createAccount();
                    case 2 -> userSignIn();
                    case 3 -> staffSignIn();
                    case 0 -> running = false;
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                ui.error(exception.getMessage());
                ui.pressEnterToContinue();
            }
        }
        ui.printHeader("GOODBYE");
        ui.info("Thank you for using the Mall Priority Parking System.");
    }

    private void createAccount() {
        ui.printHeader("CREATE ACCOUNT");
        System.out.println("User Type:\n");
        System.out.println("[1] PWD");
        System.out.println("[2] Senior Citizen");
        System.out.println("[0] Back\n");
        int typeChoice = ui.readChoice("Enter choice: ", 0, 2);
        if (typeChoice == 0) return;
        UserType type = typeChoice == 1 ? UserType.PWD : UserType.SENIOR_CITIZEN;
        String name = ui.readLine("Full Name: ");
        String eligibilityId = ui.readLine("Eligibility ID: ");
        String plate = ui.readLine("Vehicle Plate: ");
        VehicleType vehicleType = askVehicleType(true);
        if (vehicleType == null) return;
        String pin = ui.readLine("Create 4-Digit PIN: ");
        String confirmedPin = ui.readLine("Confirm PIN: ");

        PriorityUser user = accounts.registerUser(
                name, type, eligibilityId, plate, vehicleType, pin, confirmedPin);
        ui.printHeader("ACCOUNT CREATED");
        System.out.println("Status: " + status(user.getVerificationStatus()));
        System.out.println("\nYour account must be verified before you can use");
        System.out.println("priority parking.\n");
        System.out.println("Please present your valid PWD or Senior Citizen ID");
        System.out.println("to parking personnel.\n");
        ui.pressEnterToContinue();
    }

    private void userSignIn() {
        ui.printHeader("USER SIGN IN");
        System.out.println("How would you like to sign in?\n");
        System.out.println("[1] Account ID");
        System.out.println("[2] QR Token");
        System.out.println("[0] Back\n");
        int method = ui.readChoice("Enter choice: ", 0, 2);
        if (method == 0) return;
        String identifier = ui.readLine(method == 1 ? "Account ID: " : "Enter QR Token: ");
        String pin = ui.readLine("4-Digit PIN: ");
        try {
            PriorityUser user = method == 1
                    ? accounts.authenticateWithAccountId(identifier, pin)
                    : accounts.authenticateWithQr(identifier, pin);
            ui.success("Sign in successful.");
            System.out.println("\nWelcome, " + user.getName() + "!");
            System.out.println("Account Type: " + user.getUserType());
            System.out.println("Status: " + status(user.getVerificationStatus()));
            if (!user.isEligibleForPriorityParking()) {
                showRestrictedStatus(user);
                ui.pressEnterToContinue();
                return;
            }
            ui.pressEnterToContinue();
            userMenu(user);
        } catch (IllegalArgumentException exception) {
            ui.error(exception.getMessage());
            ui.pressEnterToContinue();
        }
    }

    private void showRestrictedStatus(PriorityUser user) {
        if (user.getVerificationStatus() == VerificationStatus.PENDING)
            ui.warning("Your account is still pending verification.");
        else {
            ui.warning("Your account verification was rejected.");
            if (user.getRejectionReason() != null)
                System.out.println("Reason: " + user.getRejectionReason());
        }
    }

    private void userMenu(PriorityUser user) {
        boolean loggedIn = true;
        while (loggedIn) {
            ui.printHeader("USER HOME");
            System.out.println("Welcome, " + user.getName());
            System.out.println("Account: " + user.getAccountId());
            System.out.println("Type: " + user.getUserType());
            System.out.println("Status: " + status(user.getVerificationStatus()));
            ui.printDivider();
            System.out.println("[1] Start Parking");
            System.out.println("[2] View Active Parking");
            System.out.println("[3] Parking History");
            System.out.println("[4] My Vehicles");
            System.out.println("[5] Add Vehicle");
            System.out.println("[0] Logout\n");
            int choice = ui.readChoice("Enter choice: ", 0, 5);
            try {
                switch (choice) {
                    case 1 -> startParking(user);
                    case 2 -> activeParking(user);
                    case 3 -> showUserHistory(user);
                    case 4 -> showVehicles(user);
                    case 5 -> addVehicle(user);
                    case 0 -> loggedIn = false;
                }
            } catch (IllegalArgumentException | IllegalStateException exception) {
                ui.error(exception.getMessage());
                ui.pressEnterToContinue();
            }
        }
    }

    private void startParking(PriorityUser user) {
        Vehicle vehicle = selectVehicle(user);
        if (vehicle == null) return;
        if (parking.findActiveSessionByVehicle(vehicle).isPresent()) {
            ui.warning("This vehicle already has an active parking session.");
            ui.pressEnterToContinue();
            return;
        }
        AccessibilityPreference preference = askPreference();
        if (preference == null) return;

        while (true) {
            ParkingRecommendation result;
            try {
                result = recommendations.getParkingChoices(user, vehicle, preference,
                        parking.getParkingSlots(), parking.getAllParkingHistory(manager));
            } catch (IllegalStateException exception) {
                ui.warning(exception.getMessage());
                ui.pressEnterToContinue();
                return;
            }
            ui.printHeader("AI-ASSISTED PARKING CHOICES");
            parkingConsole.displayRecommendations(result);
            System.out.println("\n[R] Refresh Recommendations");
            System.out.println("[M] View Parking Map");
            System.out.println("[0] Cancel");
            String choice = ui.readLine("\nChoose an option: ");
            if (choice.equalsIgnoreCase("R")) continue;
            if (choice.equalsIgnoreCase("M")) {
                displayParkingMap();
                continue;
            }
            if (choice.equals("0")) return;

            int optionNumber;
            try {
                optionNumber = Integer.parseInt(choice);
            } catch (NumberFormatException exception) {
                ui.error("Invalid recommendation choice.");
                continue;
            }
            if (optionNumber < 1 || optionNumber > result.getOptions().size()) {
                ui.error("Invalid recommendation choice.");
                continue;
            }
            ParkingOption option = result.getOptions().get(optionNumber - 1);
            if (option.getOptionType() == ParkingOptionType.WAIT_FOR_CLOSER_SLOT) {
                ui.info("Waiting does not reserve this parking slot.");
                ui.info("Return and refresh recommendations after the current vehicle checks out.");
                ui.pressEnterToContinue();
                return;
            }
            Duration duration = askDuration();
            if (duration == null) return;
            int confirmation = showParkingConfirmation(vehicle, option, preference, duration);
            if (confirmation == 2) continue;
            if (confirmation == 0) return;
            try {
                ParkingSession session = parking.checkIn(user, vehicle,
                        option.getParkingSlot().getSlotNumber(), preference, duration);
                showCheckInSuccess(session);
                return;
            } catch (IllegalStateException exception) {
                ui.warning("Parking conditions have changed.");
                System.out.println("Please refresh your recommendations.");
            }
        }
    }

    private Vehicle selectVehicle(PriorityUser user) {
        ui.printHeader("SELECT VEHICLE");
        System.out.println("Which vehicle are you using today?\n");
        List<Vehicle> vehicles = user.getVehicles();
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            System.out.println("[" + (i + 1) + "] " + vehicle.getPlateNumber());
            System.out.println("    Type: " + vehicle.getVehicleType() + "\n");
        }
        System.out.println("[0] Cancel\n");
        int choice = ui.readChoice("Enter choice: ", 0, vehicles.size());
        return choice == 0 ? null : vehicles.get(choice - 1);
    }

    private AccessibilityPreference askPreference() {
        ui.printHeader("ACCESSIBILITY PREFERENCE");
        System.out.println("What would you like to be closest to?\n");
        System.out.println("[1] Accessible Entrance");
        System.out.println("[2] Elevator");
        System.out.println("[3] Ramp");
        System.out.println("[4] No Preference");
        System.out.println("[0] Cancel\n");
        return switch (ui.readChoice("Enter choice: ", 0, 4)) {
            case 1 -> AccessibilityPreference.ACCESSIBLE_ENTRANCE;
            case 2 -> AccessibilityPreference.ELEVATOR;
            case 3 -> AccessibilityPreference.RAMP;
            case 4 -> AccessibilityPreference.NO_PREFERENCE;
            default -> null;
        };
    }

    private Duration askDuration() {
        ui.printHeader("ESTIMATED PARKING DURATION");
        System.out.println("[1] 30 minutes");
        System.out.println("[2] 1 hour");
        System.out.println("[3] 2 hours");
        System.out.println("[4] 3 hours");
        System.out.println("[5] 4 hours");
        System.out.println("[6] Custom minutes");
        System.out.println("[0] Cancel\n");
        return switch (ui.readChoice("Enter choice: ", 0, 6)) {
            case 1 -> Duration.ofMinutes(30);
            case 2 -> Duration.ofHours(1);
            case 3 -> Duration.ofHours(2);
            case 4 -> Duration.ofHours(3);
            case 5 -> Duration.ofHours(4);
            case 6 -> Duration.ofMinutes(ui.readPositiveNumber("Minutes: "));
            default -> null;
        };
    }

    private int showParkingConfirmation(Vehicle vehicle, ParkingOption option,
            AccessibilityPreference preference, Duration duration) {
        ParkingSlot slot = option.getParkingSlot();
        ui.printHeader("CONFIRM PARKING");
        System.out.printf("Vehicle:        %s%n", vehicle.getPlateNumber());
        System.out.printf("Slot:           %s%n", slot.getSlotNumber());
        System.out.printf("Location:       %s%n", slot.getLocation().getDescription());
        System.out.printf("Preference:     %s%n", label(preference));
        System.out.printf("Distance:       %d meters%n", slot.getLocation().getDistanceTo(preference));
        System.out.printf("Est. Duration:  %s%n", ParkingConsole.formatDuration(duration));
        ui.printDivider();
        System.out.println("[1] Confirm Check-In");
        System.out.println("[2] Choose Another Recommendation");
        System.out.println("[0] Cancel\n");
        return ui.readChoice("Enter choice: ", 0, 2);
    }

    private void showCheckInSuccess(ParkingSession session) {
        ui.printHeader("CHECK-IN SUCCESSFUL");
        ui.success("Your vehicle is now checked in.");
        System.out.printf("%nVehicle:              %s%n", session.getVehicle().getPlateNumber());
        System.out.printf("Parking Slot:         %s%n", session.getParkingSlot().getSlotNumber());
        System.out.printf("Location:             %s%n", session.getParkingSlot().getLocation().getDescription());
        System.out.printf("Check-In Time:        %s%n", session.getCheckInTime().format(TIME));
        System.out.printf("Estimated Duration:   %s%n", ParkingConsole.formatDuration(session.getEstimatedDuration()));
        System.out.printf("Estimated Departure:  %s%n", session.getEstimatedDepartureTime().format(TIME));
        ui.printDivider();
        System.out.println("Your estimated duration does not automatically check your vehicle out.\n");
        ui.pressEnterToContinue();
    }

    private void activeParking(PriorityUser user) {
        List<ParkingSession> sessions = parking.getActiveSessions().stream()
                .filter(session -> session.getUser() == user).toList();
        ui.printHeader("ACTIVE PARKING");
        if (sessions.isEmpty()) {
            ui.info("No active parking session found.");
            ui.pressEnterToContinue();
            return;
        }
        for (int i = 0; i < sessions.size(); i++)
            System.out.println("[" + (i + 1) + "] " + sessions.get(i).getVehicle().getPlateNumber()
                    + " | Slot " + sessions.get(i).getParkingSlot().getSlotNumber());
        System.out.println("[0] Back\n");
        int selected = ui.readChoice("Select session: ", 0, sessions.size());
        if (selected == 0) return;
        ParkingSession session = sessions.get(selected - 1);
        ui.printHeader("ACTIVE PARKING");
        System.out.println("Status: " + status(session.getStatus()));
        parkingConsole.displayActiveSession(session);
        ui.printDivider();
        System.out.println("[1] Check Out");
        System.out.println("[2] View Parking Map");
        System.out.println("[0] Back\n");
        int action = ui.readChoice("Enter choice: ", 0, 2);
        if (action == 1) checkOut(user, session);
        else if (action == 2) displayParkingMap();
    }

    private void checkOut(PriorityUser user, ParkingSession session) {
        ui.printHeader("CHECK OUT");
        System.out.println("Vehicle: " + session.getVehicle().getPlateNumber());
        System.out.println("Slot: " + session.getParkingSlot().getSlotNumber());
        System.out.println("\nAre you sure you want to check out?\n");
        System.out.println("[1] Yes");
        System.out.println("[0] Cancel\n");
        if (ui.readChoice("Enter choice: ", 0, 1) == 0) return;
        parking.checkOut(user, session);
        ui.printHeader("CHECK-OUT COMPLETE");
        ui.success("Parking session completed.");
        System.out.printf("%nVehicle:          %s%n", session.getVehicle().getPlateNumber());
        System.out.printf("Slot:             %s%n", session.getParkingSlot().getSlotNumber());
        System.out.printf("Check-In:         %s%n", session.getCheckInTime().format(TIME));
        System.out.printf("Check-Out:        %s%n", session.getCheckOutTime().format(TIME));
        System.out.printf("Actual Duration:  %s%n", ParkingConsole.formatDuration(session.getActualDuration()));
        System.out.println("\n" + session.getParkingSlot().getSlotNumber() + " is now "
                + status(session.getParkingSlot().getStatus()) + ".\n");
        ui.pressEnterToContinue();
    }

    private void showUserHistory(PriorityUser user) {
        ui.printHeader("PARKING HISTORY");
        parkingConsole.displayHistory(user);
        System.out.println("\n[0] Back");
        ui.readChoice("Enter choice: ", 0, 0);
    }

    private void showVehicles(PriorityUser user) {
        ui.printHeader("MY VEHICLES");
        int number = 1;
        for (Vehicle vehicle : user.getVehicles())
            System.out.println("[" + number++ + "] " + vehicle.getPlateNumber()
                    + " | " + vehicle.getVehicleType());
        System.out.println();
        ui.pressEnterToContinue();
    }

    private void addVehicle(PriorityUser user) {
        ui.printHeader("ADD VEHICLE");
        String plate = ui.readLine("Vehicle Plate: ");
        VehicleType type = askVehicleType(false);
        if (type == null) return;
        Vehicle vehicle = accounts.addVehicle(user, plate, type);
        ui.success("Vehicle " + vehicle.getPlateNumber() + " added.");
        ui.pressEnterToContinue();
    }

    private VehicleType askVehicleType(boolean allowBack) {
        System.out.println("\nVehicle Type:");
        System.out.println("[1] Car");
        System.out.println("[2] Van");
        System.out.println("[3] Motorcycle");
        if (allowBack) System.out.println("[0] Back");
        int minimum = allowBack ? 0 : 1;
        int choice = ui.readChoice("Enter choice: ", minimum, 3);
        if (choice == 0) return null;
        return VehicleType.values()[choice - 1];
    }

    private void staffSignIn() {
        ui.printHeader("STAFF SIGN IN");
        String staffId = ui.readLine("Staff ID: ");
        String password = ui.readLine("Password: ");
        try {
            Staff staff = staffService.authenticate(staffId, password);
            ui.success("Staff sign in successful.");
            System.out.println("Welcome, " + staff.getName() + "!");
            System.out.println("Role: [" + staff.getRole() + "]");
            ui.pressEnterToContinue();
            if (staff.getRole() == StaffRole.PARKING_PERSONNEL)
                personnelMenu(staff);
            else if (staff.getRole() == StaffRole.MALL_MANAGEMENT)
                managementMenu(staff);
        } catch (IllegalArgumentException exception) {
            ui.error("Invalid Staff ID or password.");
            ui.pressEnterToContinue();
        }
    }

    private void personnelMenu(Staff staff) {
        if (staff == null || staff.getRole() != StaffRole.PARKING_PERSONNEL)
            throw new IllegalArgumentException("Parking personnel access is required.");
        boolean active = true;
        while (active) {
            ui.printHeader("PARKING PERSONNEL");
            System.out.println("[1] Pending Account Verification");
            System.out.println("[2] View Parking Map");
            System.out.println("[3] Active Parking Sessions");
            System.out.println("[4] Assist Vehicle Check-Out");
            System.out.println("[5] View Parking History");
            System.out.println("[0] Logout\n");
            int choice = ui.readChoice("Enter choice: ", 0, 5);
            switch (choice) {
                case 1 -> pendingVerifications(staff);
                case 2 -> displayParkingMap();
                case 3 -> showAllActiveSessions();
                case 4 -> assistCheckout(staff);
                case 5 -> showStaffHistory(staff);
                case 0 -> active = false;
            }
        }
    }

    private void pendingVerifications(Staff staff) {
        ui.printHeader("PENDING VERIFICATIONS");
        List<PriorityUser> pending = accounts.getPendingAccounts();
        if (pending.isEmpty()) {
            ui.info("No pending verification requests.");
            ui.pressEnterToContinue();
            return;
        }
        for (int i = 0; i < pending.size(); i++) {
            PriorityUser user = pending.get(i);
            System.out.println("[" + (i + 1) + "] " + user.getName());
            System.out.println("    Type: " + user.getUserType());
            System.out.println("    Eligibility ID: " + user.getEligibilityIdNumber());
            System.out.println("    Vehicle: " + user.getVehicles().get(0).getPlateNumber() + "\n");
        }
        System.out.println("[0] Back\n");
        int choice = ui.readChoice("Select application: ", 0, pending.size());
        if (choice == 0) return;
        reviewApplication(pending.get(choice - 1), staff);
    }

    private void reviewApplication(PriorityUser user, Staff staff) {
        ui.printHeader("REVIEW APPLICATION");
        System.out.println("Name:           " + user.getName());
        System.out.println("Type:           " + user.getUserType());
        System.out.println("Eligibility ID: " + user.getEligibilityIdNumber());
        System.out.println("Vehicle:        " + user.getVehicles().get(0).getPlateNumber());
        ui.printDivider();
        System.out.println("[1] VERIFY");
        System.out.println("[2] REJECT");
        System.out.println("[0] Back\n");
        int choice = ui.readChoice("Enter choice: ", 0, 2);
        if (choice == 1) {
            accounts.verifyAccount(user, staff);
            ui.success("Account verified.");
            System.out.println("Account ID: " + user.getAccountId());
            System.out.println("QR Token: " + user.getQrToken());
        } else if (choice == 2) {
            String reason = ui.readLine("Short rejection reason: ");
            accounts.rejectAccount(user, staff, reason);
            ui.success("Account rejected.");
        }
        if (choice != 0) ui.pressEnterToContinue();
    }

    private void showAllActiveSessions() {
        ui.printHeader("ACTIVE PARKING SESSIONS");
        List<ParkingSession> active = parking.getActiveSessions();
        if (active.isEmpty()) ui.info("No active parking sessions.");
        for (int i = 0; i < active.size(); i++) {
            ParkingSession session = active.get(i);
            System.out.println("[" + (i + 1) + "] " + session.getVehicle().getPlateNumber()
                    + " | Slot " + session.getParkingSlot().getSlotNumber()
                    + " | " + status(session.getStatus()));
        }
        System.out.println();
        ui.pressEnterToContinue();
    }

    private void assistCheckout(Staff staff) {
        ui.printHeader("ASSIST VEHICLE CHECK-OUT");
        List<ParkingSession> active = parking.getActiveSessions();
        if (active.isEmpty()) {
            ui.info("No active sessions.");
            ui.pressEnterToContinue();
            return;
        }
        for (int i = 0; i < active.size(); i++)
            System.out.println("[" + (i + 1) + "] " + active.get(i).getVehicle().getPlateNumber()
                    + " | Slot " + active.get(i).getParkingSlot().getSlotNumber());
        System.out.println("[0] Back\n");
        int choice = ui.readChoice("Select session: ", 0, active.size());
        if (choice == 0) return;
        ParkingSession session = active.get(choice - 1);
        parking.checkOut(staff, session);
        ui.success("Vehicle checked out. " + session.getParkingSlot().getSlotNumber()
                + " is now [AVAILABLE].");
        ui.pressEnterToContinue();
    }

    private void managementMenu(Staff staff) {
        if (staff == null || staff.getRole() != StaffRole.MALL_MANAGEMENT)
            throw new IllegalArgumentException("Mall management access is required.");
        boolean active = true;
        while (active) {
            ui.printHeader("MALL MANAGEMENT");
            System.out.println("[1] Parking Overview");
            System.out.println("[2] Parking History");
            System.out.println("[3] Basic Reports");
            System.out.println("[0] Logout\n");
            int choice = ui.readChoice("Enter choice: ", 0, 3);
            switch (choice) {
                case 1 -> displayParkingMap();
                case 2 -> showStaffHistory(staff);
                case 3 -> showReports(staff);
                case 0 -> active = false;
            }
        }
    }

    private void displayParkingMap() {
        ui.printHeader("PARKING MAP");
        Map<String, List<ParkingSlot>> groups = new LinkedHashMap<>();
        for (ParkingSlot slot : parking.getParkingSlots()) {
            String group = slot.getLocation().getFloor().toUpperCase()
                    + " - SECTION " + slot.getLocation().getSection().toUpperCase();
            groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(slot);
        }
        groups.forEach((group, slots) -> {
            System.out.println(group);
            slots.forEach(slot -> System.out.println(
                    slot.getSlotNumber() + "   " + status(slot.getStatus())));
            System.out.println();
        });
        ui.printDivider();
        List<ParkingSlot> slots = parking.getParkingSlots();
        System.out.println("AVAILABLE : " + reports.countSlotsByStatus(slots, SlotStatus.AVAILABLE));
        System.out.println("OCCUPIED  : " + reports.countSlotsByStatus(slots, SlotStatus.OCCUPIED));
        System.out.println("RESERVED  : " + reports.countSlotsByStatus(slots, SlotStatus.RESERVED));
        System.out.println("\n[0] Back");
        ui.readChoice("Enter choice: ", 0, 0);
    }

    private void showStaffHistory(Staff staff) {
        ui.printHeader("PARKING HISTORY");
        List<ParkingSession> history = parking.getAllParkingHistory(staff);
        if (history.isEmpty()) ui.info("No completed parking sessions found.");
        for (int i = 0; i < history.size(); i++) {
            ParkingSession session = history.get(i);
            System.out.println("Entry #" + (i + 1));
            System.out.println("User: " + session.getUser().getName());
            System.out.println("Vehicle: " + session.getVehicle().getPlateNumber());
            System.out.println("Slot: " + session.getParkingSlot().getSlotNumber());
            System.out.println("Duration: " + ParkingConsole.formatDuration(session.getActualDuration()));
            ui.printDivider();
        }
        System.out.println("[0] Back");
        ui.readChoice("Enter choice: ", 0, 0);
    }

    private void showReports(Staff staff) {
        ui.printHeader("BASIC REPORTS");
        List<ParkingSlot> slots = parking.getParkingSlots();
        List<ParkingSession> history = parking.getAllParkingHistory(staff);
        System.out.println("Total Slots:              " + slots.size());
        System.out.println("Available:                " + reports.countSlotsByStatus(slots, SlotStatus.AVAILABLE));
        System.out.println("Occupied:                 " + reports.countSlotsByStatus(slots, SlotStatus.OCCUPIED));
        System.out.println("Reserved:                 " + reports.countSlotsByStatus(slots, SlotStatus.RESERVED));
        System.out.println("Active Sessions:          " + parking.getActiveSessions().size());
        System.out.println("Completed Sessions:       " + history.size());
        System.out.println("Average Parking Duration: "
                + ParkingConsole.formatDuration(reports.calculateAverageParkingDuration(history)));
        System.out.println();
        ui.pressEnterToContinue();
    }

    private String status(Enum<?> value) { return "[" + value + "]"; }

    private String label(AccessibilityPreference preference) {
        return switch (preference) {
            case ACCESSIBLE_ENTRANCE -> "Accessible Entrance";
            case ELEVATOR -> "Elevator";
            case RAMP -> "Ramp";
            case NO_PREFERENCE -> "No Preference";
        };
    }
}
