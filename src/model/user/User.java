package model.user;
import java.util.Objects;
public abstract class User {
    private final String userId;
    private String name;
    protected User(String id, String name) { this.userId = text(id, "User ID"); this.name = text(name, "Name"); }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = text(name, "Name"); }
    protected static String text(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }
    @Override public boolean equals(Object other) {
        return this == other || (other instanceof User user && userId.equals(user.userId));
    }
    @Override public int hashCode() { return Objects.hash(userId); }
}
