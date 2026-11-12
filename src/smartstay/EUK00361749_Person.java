package smartstay;

/**
 * Abstract base class for every person who uses SmartStay.
 *
 * <p>Guests and administrators share a full name, contact number and email address, so these
 * fields and their validation live here once (inheritance). Each subclass must describe its own
 * role, which is used polymorphically by the user interface.
 */
public abstract class EUK00361749_Person {

    private final String fullName;
    private final String contactNumber;
    private final String email;

    /**
     * Validates and stores the details common to all people in the system.
     *
     * @param fullName      1-100 printable characters
     * @param contactNumber 7-15 digits with an optional leading "+"
     * @param email         a basic name@domain.tld address
     * @throws IllegalArgumentException if any value is invalid
     */
    protected EUK00361749_Person(String fullName, String contactNumber, String email) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 100
                || fullName.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Full name must contain 1-100 printable characters.");
        }
        if (contactNumber == null || !contactNumber.trim().matches("[+]?[0-9]{7,15}")) {
            throw new IllegalArgumentException("Contact number must be 7-15 digits (optional leading +).");
        }
        if (email == null || email.length() > 254
                || !email.trim().matches("[^\\s@]+@[^\\s@]+[.][^\\s@]+")) {
            throw new IllegalArgumentException("Enter a valid email address, e.g. name@example.com.");
        }
        this.fullName = fullName.trim();
        this.contactNumber = contactNumber.trim();
        this.email = email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public String getFullName() {
        return fullName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getEmail() {
        return email;
    }

    /** @return a short role label such as "Guest" or "Administrator" (overridden by subclasses). */
    public abstract String getRole();

    /** @return the unique identifier for this person (NIC/passport for guests, username for admins). */
    public abstract String getIdentifier();

    /** One-line summary used in listings; works for any subclass through polymorphism. */
    @Override
    public String toString() {
        return String.format("%-10s | %-14s | %-25s | %-14s | %s",
                getRole(), getIdentifier(), fullName, contactNumber, email);
    }
}
