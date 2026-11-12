package smartstay;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * All SmartStay business rules, kept separate from the console so they can be tested directly.
 *
 * <p>A {@link Clock} is injected instead of reading the system time, which lets the tests fix
 * "now" and check the 2-day and 24-hour boundaries exactly.
 */
public final class EUK00361749_Service {

    public static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Colombo");
    /** Standard check-in time used for the 24-hour cancellation rule (assumption). */
    public static final LocalTime CHECK_IN_TIME = LocalTime.of(14, 0);
    /** Guests must book at least this many calendar days in advance. */
    public static final int MINIMUM_DAYS_IN_ADVANCE = 2;
    /** Changes are blocked within this many hours of check-in. */
    public static final int CHANGE_CUTOFF_HOURS = 24;

    private static final String BOOKING_ID_PREFIX = "BK";

    private final EUK00361749_Store store;
    private final Clock clock;
    private final EUK00361749_DiscountPolicy discountPolicy;

    /**
     * @param store          where records are held
     * @param clock          source of the current time (fixed in tests)
     * @param discountPolicy discount rule applied to invoices (Strategy pattern)
     */
    public EUK00361749_Service(EUK00361749_Store store, Clock clock, EUK00361749_DiscountPolicy discountPolicy) {
        this.store = Objects.requireNonNull(store);
        this.clock = clock.withZone(HOTEL_ZONE);
        this.discountPolicy = Objects.requireNonNull(discountPolicy);
    }

    // =====================================================================
    // Administrator functions
    // =====================================================================

    /** Adds a new room; the room ID must be unique. */
    public void addRoom(EUK00361749_Room room) {
        if (store.findRoom(room.getRoomId()) != null) {
            throw new IllegalArgumentException("Room " + room.getRoomId() + " already exists.");
        }
        store.saveRoom(room);
    }

    /**
     * Replaces a room's type, bed and status. Blocked while a current or future confirmed booking
     * uses the room, because changing the type would change a price the guest already agreed to.
     */
    public void updateRoom(EUK00361749_Room updatedRoom) {
        findRoomOrFail(updatedRoom.getRoomId());
        if (hasCurrentOrFutureBooking(updatedRoom.getRoomId())) {
            throw new IllegalArgumentException("Room has current or future bookings; only its status can be changed.");
        }
        store.saveRoom(updatedRoom);
    }

    /** Changes only the availability status (Available / Reserved / Under Maintenance). */
    public EUK00361749_Room changeRoomStatus(String roomId, EUK00361749_RoomStatus newStatus) {
        EUK00361749_Room updated = findRoomOrFail(roomId).withStatus(newStatus);
        store.saveRoom(updated);
        return updated;
    }

    /** Removes a room. Rooms with any booking history are kept so old invoices stay valid. */
    public void removeRoom(String roomId) {
        EUK00361749_Room room = findRoomOrFail(roomId);
        for (EUK00361749_Booking booking : store.getAllBookings()) {
            if (booking.getRoom().getRoomId().equals(room.getRoomId())) {
                throw new IllegalArgumentException("Room has booking history. Set it to Under Maintenance instead.");
            }
        }
        store.deleteRoom(room.getRoomId());
    }

    public List<EUK00361749_Room> getAllRooms() {
        return store.getAllRooms();
    }

    public List<EUK00361749_Guest> getAllGuests() {
        return store.getAllGuests();
    }

    // =====================================================================
    // Guest functions
    // =====================================================================

    /** Registers a guest; the NIC/passport must not already be registered. */
    public void registerGuest(EUK00361749_Guest guest) {
        if (store.findGuest(guest.getNicOrPassport()) != null) {
            throw new IllegalArgumentException("A guest with this NIC/Passport is already registered.");
        }
        store.saveGuest(guest);
    }

    public EUK00361749_Guest findGuest(String nicOrPassport) {
        return store.findGuest(nicOrPassport);
    }

    /**
     * Search algorithm: linear scan over all rooms, keeping those of the requested type that are
     * free for the whole stay. O(rooms x bookings), which is fine for one hotel's inventory.
     */
    public List<EUK00361749_Room> searchAvailableRooms(EUK00361749_RoomType roomType,
                                                       LocalDate checkIn, LocalDate checkOut) {
        validateStayDates(checkIn, checkOut);
        List<EUK00361749_Room> availableRooms = new ArrayList<>();
        for (EUK00361749_Room room : store.getAllRooms()) {
            if (room.getRoomType() == roomType && isRoomFree(room, checkIn, checkOut, null)) {
                availableRooms.add(room);
            }
        }
        return availableRooms;
    }

    /**
     * Builds an invoice preview for a proposed stay without saving anything, so the guest can see
     * the price before confirming.
     */
    public EUK00361749_Invoice quoteReservation(String nicOrPassport, String roomId, int numberOfGuests,
                                                LocalDate checkIn, LocalDate checkOut) {
        EUK00361749_Booking proposed = buildValidatedBooking("QUOTE", nicOrPassport, roomId,
                numberOfGuests, checkIn, checkOut, null);
        return new EUK00361749_Invoice(proposed, discountPolicy);
    }

    /** Creates and stores a confirmed reservation with the next sequential booking ID. */
    public EUK00361749_Booking makeReservation(String nicOrPassport, String roomId, int numberOfGuests,
                                               LocalDate checkIn, LocalDate checkOut) {
        EUK00361749_Booking booking = buildValidatedBooking(generateNextBookingId(), nicOrPassport, roomId,
                numberOfGuests, checkIn, checkOut, null);
        store.saveBooking(booking);
        return booking;
    }

    /** Cancels a reservation if it is more than 24 hours before check-in. */
    public EUK00361749_Booking cancelReservation(String bookingId) {
        EUK00361749_Booking booking = findBookingOrFail(bookingId);
        ensureChangeAllowed(booking);
        EUK00361749_Booking cancelled = booking.cancel();
        store.saveBooking(cancelled);
        return cancelled;
    }

    /** Changes room, guests or dates of a reservation, keeping the same booking ID. */
    public EUK00361749_Booking modifyReservation(String bookingId, String newRoomId, int numberOfGuests,
                                                 LocalDate checkIn, LocalDate checkOut) {
        EUK00361749_Booking existing = findBookingOrFail(bookingId);
        ensureChangeAllowed(existing);
        EUK00361749_Booking revised = buildValidatedBooking(existing.getBookingId(),
                existing.getGuest().getNicOrPassport(), newRoomId, numberOfGuests, checkIn, checkOut,
                existing.getBookingId());
        store.saveBooking(revised);
        return revised;
    }

    /**
     * Search algorithm: linear search matching either the exact booking ID or any part of the
     * guest's name (case-insensitive).
     */
    public List<EUK00361749_Booking> searchReservations(String guestNameOrBookingId) {
        String query = guestNameOrBookingId == null ? "" : guestNameOrBookingId.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Enter a guest name or booking ID.");
        }
        List<EUK00361749_Booking> matches = new ArrayList<>();
        for (EUK00361749_Booking booking : store.getAllBookings()) {
            boolean idMatches = booking.getBookingId().equalsIgnoreCase(query);
            boolean nameMatches = booking.getGuest().getFullName().toLowerCase(Locale.ROOT).contains(query);
            if (idMatches || nameMatches) {
                matches.add(booking);
            }
        }
        return EUK00361749_BookingSorter.sortByCheckInDate(matches);
    }

    /**
     * Lists reservations for one check-in date, or all reservations when the date is null, sorted
     * by check-in date with the hand-written merge sort.
     */
    public List<EUK00361749_Booking> viewReservationsByCheckIn(LocalDate checkInDate) {
        List<EUK00361749_Booking> selected = new ArrayList<>();
        for (EUK00361749_Booking booking : store.getAllBookings()) {
            if (checkInDate == null || booking.getCheckInDate().equals(checkInDate)) {
                selected.add(booking);
            }
        }
        return EUK00361749_BookingSorter.sortByCheckInDate(selected);
    }

    /** Generates the detailed invoice for an active booking. */
    public EUK00361749_Invoice generateInvoice(String bookingId) {
        return new EUK00361749_Invoice(findBookingOrFail(bookingId), discountPolicy);
    }

    public EUK00361749_Booking findBookingOrFail(String bookingId) {
        EUK00361749_Booking booking = store.findBooking(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking " + bookingId.trim().toUpperCase(Locale.ROOT) + " not found.");
        }
        return booking;
    }

    // =====================================================================
    // Validation helpers
    // =====================================================================

    /** Applies every booking rule and returns a booking object (not yet stored). */
    private EUK00361749_Booking buildValidatedBooking(String bookingId, String nicOrPassport, String roomId,
                                                      int numberOfGuests, LocalDate checkIn, LocalDate checkOut,
                                                      String bookingIdToIgnore) {
        validateStayDates(checkIn, checkOut);
        EUK00361749_Guest guest = store.findGuest(nicOrPassport);
        if (guest == null) {
            throw new IllegalArgumentException("Guest is not registered. Please register first.");
        }
        EUK00361749_Room room = findRoomOrFail(roomId);
        if (!isRoomFree(room, checkIn, checkOut, bookingIdToIgnore)) {
            throw new IllegalArgumentException("Room " + room.getRoomId() + " is not available for these dates.");
        }
        return new EUK00361749_Booking.EUK00361749_BookingBuilder(bookingId, guest, room)
                .stayDates(checkIn, checkOut)
                .numberOfGuests(numberOfGuests)
                .build();
    }

    /** Check-out must follow check-in, and check-in must be at least 2 calendar days ahead. */
    private void validateStayDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after the check-in date.");
        }
        LocalDate earliestCheckIn = LocalDate.now(clock).plusDays(MINIMUM_DAYS_IN_ADVANCE);
        if (checkIn.isBefore(earliestCheckIn)) {
            throw new IllegalArgumentException("Bookings must be made at least " + MINIMUM_DAYS_IN_ADVANCE
                    + " days in advance (earliest check-in " + earliestCheckIn + ").");
        }
    }

    /**
     * A room is free when its status is Available and no confirmed booking overlaps the stay.
     *
     * @param bookingIdToIgnore booking being modified (it must not clash with itself), or null
     */
    private boolean isRoomFree(EUK00361749_Room room, LocalDate checkIn, LocalDate checkOut,
                               String bookingIdToIgnore) {
        if (room.getStatus() != EUK00361749_RoomStatus.AVAILABLE) {
            return false;
        }
        for (EUK00361749_Booking booking : store.getAllBookings()) {
            boolean sameRoom = booking.getRoom().getRoomId().equals(room.getRoomId());
            boolean ignored = booking.getBookingId().equalsIgnoreCase(String.valueOf(bookingIdToIgnore));
            if (sameRoom && !ignored && booking.overlaps(checkIn, checkOut)) {
                return false;
            }
        }
        return true;
    }

    /** Blocks changes and cancellations within 24 hours of the 14:00 check-in time. */
    private void ensureChangeAllowed(EUK00361749_Booking booking) {
        if (booking.isCancelled()) {
            throw new IllegalArgumentException("Booking " + booking.getBookingId() + " is already cancelled.");
        }
        Instant checkInMoment = booking.getCheckInDate().atTime(CHECK_IN_TIME).atZone(HOTEL_ZONE).toInstant();
        Instant cutoff = checkInMoment.minus(Duration.ofHours(CHANGE_CUTOFF_HOURS));
        if (clock.instant().isAfter(cutoff)) {
            throw new IllegalArgumentException("Bookings cannot be changed or cancelled within "
                    + CHANGE_CUTOFF_HOURS + " hours of check-in.");
        }
    }

    private boolean hasCurrentOrFutureBooking(String roomId) {
        LocalDate today = LocalDate.now(clock);
        for (EUK00361749_Booking booking : store.getAllBookings()) {
            if (booking.getRoom().getRoomId().equalsIgnoreCase(roomId.trim())
                    && !booking.isCancelled() && booking.getCheckOutDate().isAfter(today)) {
                return true;
            }
        }
        return false;
    }

    private EUK00361749_Room findRoomOrFail(String roomId) {
        EUK00361749_Room room = store.findRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room " + roomId.trim().toUpperCase(Locale.ROOT) + " not found.");
        }
        return room;
    }

    /** Next ID in the sequence BK0001, BK0002, ... based on the highest number already used. */
    private String generateNextBookingId() {
        int highestNumber = 0;
        for (EUK00361749_Booking booking : store.getAllBookings()) {
            String id = booking.getBookingId();
            if (id.matches(BOOKING_ID_PREFIX + "\\d+")) {
                highestNumber = Math.max(highestNumber, Integer.parseInt(id.substring(BOOKING_ID_PREFIX.length())));
            }
        }
        return String.format("%s%04d", BOOKING_ID_PREFIX, highestNumber + 1);
    }
}
