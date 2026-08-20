package service;

import enums.*;
import model.Vehicle;
import model.user.*;
import java.time.LocalDateTime;
import java.util.*;

public class AccountService {
    private final List<PriorityUser> accounts = new ArrayList<>();
    private int nextUserNumber = 1;
    private int nextAccountNumber = 10001;

    public PriorityUser registerUser(String name, UserType type, String eligibilityId,
                                     String pin, String confirmedPin) {
        if (type == null) throw new IllegalArgumentException("A valid user type is required.");
        if (pin == null || !pin.matches("\\d{4}"))
            throw new IllegalArgumentException("PIN must contain exactly four digits.");
        if (!pin.equals(confirmedPin)) throw new IllegalArgumentException("PIN confirmation does not match.");
        if (eligibilityId == null || eligibilityId.isBlank())
            throw new IllegalArgumentException("Eligibility ID is required.");
        if (accounts.stream().anyMatch(a -> a.getEligibilityIdNumber().equalsIgnoreCase(eligibilityId.trim())))
            throw new IllegalArgumentException("Eligibility ID is already registered.");

        String userId = String.format("U-%04d", nextUserNumber++);
        PriorityUser user = switch (type) {
            case PWD -> new PWDUser(userId, name, eligibilityId, pin);
            case SENIOR_CITIZEN -> new SeniorCitizenUser(userId, name, eligibilityId, pin);
        };
        accounts.add(user);
        return user;
    }

    public PriorityUser registerUser(String name, UserType type, String eligibilityId,
                                     String plate, VehicleType vehicleType,
                                     String pin, String confirmedPin) {
        // Validate the plate first so a failed first-vehicle registration does not
        // leave behind an incomplete account.
        validateUniquePlate(plate);
        PriorityUser user = registerUser(name, type, eligibilityId, pin, confirmedPin);
        registerVehicle(user, plate, vehicleType);
        return user;
    }

    public Vehicle registerVehicle(PriorityUser owner, String plate, VehicleType type) {
        requireKnownAccount(owner);
        validateUniquePlate(plate);
        Vehicle vehicle = new Vehicle(plate, type, owner);
        owner.addVehicle(vehicle);
        return vehicle;
    }

    public Vehicle addVehicle(PriorityUser owner, String plate, VehicleType type) {
        requireKnownAccount(owner);
        if (!owner.isEligibleForPriorityParking())
            throw new IllegalStateException("Only verified users can add vehicles after registration.");
        return registerVehicle(owner, plate, type);
    }

    public void verifyAccount(PriorityUser user, Staff staff) {
        requireKnownAccount(user);
        requireParkingPersonnel(staff);
        String accountId = String.format("MP-%05d", nextAccountNumber++);
        String qrToken;
        do {
            qrToken = "PRK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (findByQrToken(qrToken).isPresent());
        user.markVerified(accountId, qrToken, staff, LocalDateTime.now());
    }

    public void rejectAccount(PriorityUser user, Staff staff, String reason) {
        requireKnownAccount(user);
        requireParkingPersonnel(staff);
        user.markRejected(reason);
    }

    public Optional<PriorityUser> findByAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) return Optional.empty();
        return accounts.stream().filter(a -> a.getAccountId() != null
                && a.getAccountId().equalsIgnoreCase(accountId.trim())).findFirst();
    }

    public Optional<PriorityUser> findByQrToken(String qrToken) {
        if (qrToken == null || qrToken.isBlank()) return Optional.empty();
        return accounts.stream().filter(a -> a.getQrToken() != null
                && a.getQrToken().equalsIgnoreCase(qrToken.trim())).findFirst();
    }

    public PriorityUser authenticateWithAccountId(String accountId, String pin) {
        return authenticate(findByAccountId(accountId), pin, "Unknown Account ID.");
    }

    public PriorityUser authenticateWithQr(String qrToken, String pin) {
        return authenticate(findByQrToken(qrToken), pin, "Unknown QR token.");
    }

    public List<PriorityUser> getPendingAccounts() {
        return accounts.stream().filter(a -> a.getVerificationStatus() == VerificationStatus.PENDING).toList();
    }

    public List<PriorityUser> getAccounts() { return List.copyOf(accounts); }

    private PriorityUser authenticate(Optional<PriorityUser> result, String pin, String unknownMessage) {
        PriorityUser account = result.orElseThrow(() -> new IllegalArgumentException(unknownMessage));
        if (!account.verifyPin(pin)) throw new IllegalArgumentException("Incorrect PIN.");
        return account;
    }

    private void validateUniquePlate(String plate) {
        if (plate == null || plate.isBlank()) throw new IllegalArgumentException("Plate number is required.");
        if (accounts.stream().flatMap(a -> a.getVehicles().stream())
                .anyMatch(v -> v.getPlateNumber().equalsIgnoreCase(plate.trim())))
            throw new IllegalArgumentException("Plate number is already registered.");
    }

    private void requireKnownAccount(PriorityUser user) {
        if (user == null || !accounts.contains(user)) throw new IllegalArgumentException("Account is not registered.");
    }

    private void requireParkingPersonnel(Staff staff) {
        if (staff == null || staff.getRole() != StaffRole.PARKING_PERSONNEL)
            throw new IllegalArgumentException("Parking personnel authorization is required.");
    }
}
