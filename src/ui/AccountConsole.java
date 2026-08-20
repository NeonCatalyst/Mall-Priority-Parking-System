package ui;

import enums.UserType;
import enums.VehicleType;
import enums.VerificationStatus;
import model.Vehicle;
import model.user.PriorityUser;
import model.user.Staff;
import service.AccountService;

import java.util.List;
import java.util.Scanner;

public class AccountConsole {
    private final AccountService accountService;
    private final Scanner scanner;

    public AccountConsole(AccountService accountService, Scanner scanner) {
        this.accountService = accountService;
        this.scanner = scanner;
    }

    public PriorityUser registerUser() {
        System.out.println("\n=== CREATE ACCOUNT ===");
        System.out.print("Full name: ");
        String name = scanner.nextLine();
        System.out.print("User type (1 = PWD, 2 = Senior Citizen): ");
        UserType type = readUserType(scanner.nextLine());
        System.out.print("Eligibility ID number: ");
        String eligibilityId = scanner.nextLine();
        System.out.print("Vehicle plate number: ");
        String plate = scanner.nextLine();
        System.out.print("Vehicle type (1 = Car, 2 = Van, 3 = Motorcycle): ");
        VehicleType vehicleType = readVehicleType(scanner.nextLine());
        System.out.print("Create 4-digit PIN: ");
        String pin = scanner.nextLine();
        System.out.print("Confirm PIN: ");
        String confirmedPin = scanner.nextLine();

        PriorityUser user = accountService.registerUser(
                name, type, eligibilityId, plate, vehicleType, pin, confirmedPin);
        System.out.println("Registration complete. Status: " + user.getVerificationStatus());
        System.out.println("Application ID for staff review: " + user.getUserId());
        return user;
    }

    public PriorityUser returningUserLogin() {
        System.out.println("\n=== RETURNING USER LOGIN ===");
        System.out.print("Login with 1 = QR token or 2 = Account ID: ");
        String method = scanner.nextLine();
        System.out.print(method.equals("1") ? "QR token: " : "Account ID: ");
        String identifier = scanner.nextLine();
        System.out.print("4-digit PIN: ");
        String pin = scanner.nextLine();
        PriorityUser user = method.equals("1")
                ? accountService.authenticateWithQr(identifier, pin)
                : accountService.authenticateWithAccountId(identifier, pin);
        displayAuthenticationResult(user);
        return user;
    }

    public Vehicle selectVehicle(PriorityUser user) {
        if (!user.isEligibleForPriorityParking())
            throw new IllegalStateException("Vehicle selection requires a verified account.");
        List<Vehicle> vehicles = user.getVehicles();
        System.out.println("\nRegistered Vehicles:");
        for (int index = 0; index < vehicles.size(); index++)
            System.out.println((index + 1) + ". " + vehicles.get(index).getDisplayName());
        System.out.print("Select vehicle number: ");
        int choice = Integer.parseInt(scanner.nextLine());
        if (choice < 1 || choice > vehicles.size()) throw new IllegalArgumentException("Invalid vehicle selection.");
        return vehicles.get(choice - 1);
    }

    public void reviewPendingAccounts(Staff staff) {
        List<PriorityUser> pending = accountService.getPendingAccounts();
        System.out.println("\n=== PENDING ACCOUNTS ===");
        if (pending.isEmpty()) {
            System.out.println("No pending accounts.");
            return;
        }
        for (int index = 0; index < pending.size(); index++) {
            PriorityUser user = pending.get(index);
            System.out.println((index + 1) + ". " + user.getName() + " | " + user.getUserType()
                    + " | ID: " + user.getEligibilityIdNumber());
        }
        System.out.print("Select account number: ");
        int choice = Integer.parseInt(scanner.nextLine());
        if (choice < 1 || choice > pending.size()) throw new IllegalArgumentException("Invalid account selection.");
        PriorityUser selected = pending.get(choice - 1);
        System.out.print("Enter V to verify or R to reject: ");
        if (scanner.nextLine().equalsIgnoreCase("V")) {
            accountService.verifyAccount(selected, staff);
            System.out.println("Verified. Account ID: " + selected.getAccountId());
            System.out.println("QR token: " + selected.getQrToken());
        } else {
            System.out.print("Short rejection reason: ");
            accountService.rejectAccount(selected, staff, scanner.nextLine());
            System.out.println("Account rejected.");
        }
    }

    public void displayAuthenticationResult(PriorityUser user) {
        if (user.getVerificationStatus() == VerificationStatus.PENDING) {
            System.out.println("Your account is still pending verification. "
                    + "Please present your PWD or Senior Citizen ID to parking personnel.");
        } else if (user.getVerificationStatus() == VerificationStatus.REJECTED) {
            System.out.println("Your account verification was rejected.");
            if (user.getRejectionReason() != null) System.out.println("Reason: " + user.getRejectionReason());
        } else {
            System.out.println("Welcome, " + user.getName() + "!");
            System.out.println("Verified " + user.getUserType());
            user.getVehicles().forEach(vehicle -> System.out.println("- " + vehicle.getDisplayName()));
        }
    }

    private UserType readUserType(String choice) {
        return switch (choice) {
            case "1" -> UserType.PWD;
            case "2" -> UserType.SENIOR_CITIZEN;
            default -> throw new IllegalArgumentException("Invalid user type.");
        };
    }

    private VehicleType readVehicleType(String choice) {
        return switch (choice) {
            case "1" -> VehicleType.CAR;
            case "2" -> VehicleType.VAN;
            case "3" -> VehicleType.MOTORCYCLE;
            default -> throw new IllegalArgumentException("Invalid vehicle type.");
        };
    }
}
