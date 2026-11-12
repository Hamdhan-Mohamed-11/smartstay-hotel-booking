package smartstay;

/**
 * Availability status set by the administrator. Only {@link #AVAILABLE} rooms can be reserved;
 * date clashes with existing bookings are checked separately by the service.
 */
public enum EUK00361749_RoomStatus {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    UNDER_MAINTENANCE("Under Maintenance");

    private final String displayName;

    EUK00361749_RoomStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
