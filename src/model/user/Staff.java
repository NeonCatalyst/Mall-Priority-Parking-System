package model.user;
import enums.StaffRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class Staff extends User {
    private final StaffRole role;
    private final String passwordHash;
    public Staff(String id, String name, StaffRole role, String password) {
        super(id, name);
        if (role == null) throw new IllegalArgumentException("Role is required.");
        if (password == null || password.length() < 4)
            throw new IllegalArgumentException("Staff password must contain at least four characters.");
        this.role = role;
        this.passwordHash = hash(password);
    }
    public StaffRole getRole() { return role; }
    public boolean verifyPassword(String password) {
        return password != null && passwordHash.equals(hash(password));
    }
    private static String hash(String password) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable.", exception);
        }
    }
}
