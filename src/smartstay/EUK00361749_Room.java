package smartstay;

import java.util.Locale;
import java.util.Objects;

/**
 * A single hotel room. Rooms are immutable: an update replaces the object stored under the same
 * room ID, which keeps old bookings pointing at the room details they were made with.
 */
public final class EUK00361749_Room {

    private final String roomId;
    private final EUK00361749_RoomType roomType;
    private final EUK00361749_BedType bedType;
    private final EUK00361749_RoomStatus status;

    /**
     * @param roomId   1-20 letters, digits or hyphens (stored in upper case)
     * @param roomType package that sets the daily rate and tax
     * @param bedType  bed configuration
     * @param status   availability status set by the administrator
     */
    public EUK00361749_Room(String roomId, EUK00361749_RoomType roomType,
                            EUK00361749_BedType bedType, EUK00361749_RoomStatus status) {
        if (roomId == null || !roomId.trim().matches("[A-Za-z0-9-]{1,20}")) {
            throw new IllegalArgumentException("Room ID must be 1-20 letters, digits or hyphens.");
        }
        this.roomId = roomId.trim().toUpperCase(Locale.ROOT);
        this.roomType = Objects.requireNonNull(roomType, "Room type is required.");
        this.bedType = Objects.requireNonNull(bedType, "Bed type is required.");
        this.status = Objects.requireNonNull(status, "Status is required.");
    }

    public String getRoomId() {
        return roomId;
    }

    public EUK00361749_RoomType getRoomType() {
        return roomType;
    }

    public EUK00361749_BedType getBedType() {
        return bedType;
    }

    public EUK00361749_RoomStatus getStatus() {
        return status;
    }

    /** Returns a copy of this room with a different status (rooms are immutable). */
    public EUK00361749_Room withStatus(EUK00361749_RoomStatus newStatus) {
        return new EUK00361749_Room(roomId, roomType, bedType, newStatus);
    }

    @Override
    public String toString() {
        return String.format("%-8s | %-12s | %-6s | LKR %,10.2f | up to %d guests | %-9s | %s",
                roomId, roomType, bedType, roomType.getDailyRate(), roomType.getIncludedGuests(),
                roomType.hasComplimentaryBreakfast() ? "Yes" : "No", status);
    }
}
