package smartstay;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * A hotel administrator who manages room records. Only a SHA-256 hash of the password is kept in
 * memory, so the plain password is never stored.
 */
public final class EUK00361749_Admin extends EUK00361749_Person {

    private final String username;
    private final String passwordHash;

    /**
     * Creates an administrator account.
     *
     * @param username      login name (letters and digits)
     * @param password      plain-text password, hashed immediately
     * @param fullName      administrator's name
     * @param contactNumber administrator's phone number
     * @param email         administrator's email address
     */
    public EUK00361749_Admin(String username, String password, String fullName,
                             String contactNumber, String email) {
        super(fullName, contactNumber, email);
        if (username == null || !username.matches("[A-Za-z0-9]{3,20}")) {
            throw new IllegalArgumentException("Username must be 3-20 letters or digits.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must have at least 6 characters.");
        }
        this.username = username;
        this.passwordHash = hashPassword(password);
    }

    public String getUsername() {
        return username;
    }

    /** @return true when the supplied username and password match this account. */
    public boolean checkCredentials(String enteredUsername, String enteredPassword) {
        if (enteredUsername == null || enteredPassword == null) {
            return false;
        }
        // MessageDigest.isEqual compares in constant time to avoid leaking timing information.
        return username.equalsIgnoreCase(enteredUsername.trim())
                && MessageDigest.isEqual(
                        passwordHash.getBytes(StandardCharsets.UTF_8),
                        hashPassword(enteredPassword).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getRole() {
        return "Admin";
    }

    @Override
    public String getIdentifier() {
        return username;
    }

    /** Converts a password to a hexadecimal SHA-256 digest. */
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available on this JVM.", ex);
        }
    }
}
