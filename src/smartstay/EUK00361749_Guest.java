package smartstay;

import java.util.Locale;

/**
 * A registered hotel guest. The NIC or passport number is the unique key used to find the guest
 * when a reservation is made. Instances are immutable once validated.
 */
public final class EUK00361749_Guest extends EUK00361749_Person {

    private final String nicOrPassport;

    /**
     * Registers a guest with the four details required by the business rules.
     *
     * @param nicOrPassport 5-20 letters or digits (stored in upper case)
     * @param fullName      guest's full name
     * @param contactNumber guest's phone number
     * @param email         guest's email address
     */
    public EUK00361749_Guest(String nicOrPassport, String fullName, String contactNumber, String email) {
        super(fullName, contactNumber, email);
        if (nicOrPassport == null || !nicOrPassport.trim().matches("[A-Za-z0-9]{5,20}")) {
            throw new IllegalArgumentException("NIC/Passport must be 5-20 letters or digits.");
        }
        this.nicOrPassport = nicOrPassport.trim().toUpperCase(Locale.ROOT);
    }

    public String getNicOrPassport() {
        return nicOrPassport;
    }

    @Override
    public String getRole() {
        return "Guest";
    }

    @Override
    public String getIdentifier() {
        return nicOrPassport;
    }
}
