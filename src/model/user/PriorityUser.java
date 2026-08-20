package model.user;

import enums.UserType;
import enums.VerificationStatus;
import model.Vehicle;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public abstract class PriorityUser extends User {
    private String accountId;
    private final String eligibilityIdNumber;
    private String qrToken;
    private String pinHash;
    private VerificationStatus status = VerificationStatus.PENDING;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private Staff verifiedBy;
    private LocalDateTime verificationDateTime;
    private String rejectionReason;

    protected PriorityUser(String userId, String name, String eligibilityIdNumber, String pin) {
        super(userId, name);
        this.eligibilityIdNumber = text(eligibilityIdNumber, "Eligibility ID");
        changePin(pin);
    }

    public abstract UserType getUserType();
    public String getAccountId() { return accountId; }
    public String getQrToken() { return qrToken; }
    public String getEligibilityIdNumber() { return eligibilityIdNumber; }
    public VerificationStatus getVerificationStatus() { return status; }
    public Staff getVerifiedBy() { return verifiedBy; }
    public LocalDateTime getVerificationDateTime() { return verificationDateTime; }
    public String getRejectionReason() { return rejectionReason; }
    public boolean isEligibleForPriorityParking() { return status == VerificationStatus.VERIFIED; }
    public boolean verifyPin(String pin) { return validPin(pin) && pinHash.equals(hash(pin)); }
    public void changePin(String pin) {
        if (!validPin(pin)) throw new IllegalArgumentException("PIN must contain exactly four digits.");
        pinHash = hash(pin);
    }
    public void addVehicle(Vehicle vehicle) {
        if (vehicle == null || vehicle.getOwner() != this) throw new IllegalArgumentException("Vehicle must belong to this user.");
        if (vehicles.stream().anyMatch(v -> v.getPlateNumber().equalsIgnoreCase(vehicle.getPlateNumber())))
            throw new IllegalArgumentException("Vehicle is already registered.");
        vehicles.add(vehicle);
    }
    public List<Vehicle> getVehicles() { return List.copyOf(vehicles); }
    public void markVerified(String accountId, String qrToken, Staff staff, LocalDateTime time) {
        if (status != VerificationStatus.PENDING) throw new IllegalStateException("Only pending accounts can be verified.");
        this.accountId=text(accountId,"Account ID"); this.qrToken=text(qrToken,"QR token");
        this.verifiedBy=staff; this.verificationDateTime=time; this.rejectionReason=null;
        status=VerificationStatus.VERIFIED;
    }
    public void markRejected(String reason) {
        if (status != VerificationStatus.PENDING) throw new IllegalStateException("Only pending accounts can be rejected.");
        this.rejectionReason=text(reason,"Rejection reason");
        status=VerificationStatus.REJECTED;
    }
    private static boolean validPin(String pin) { return pin != null && pin.matches("\\d{4}"); }
    private static String hash(String pin) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(pin.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable.", e);
        }
    }
}
