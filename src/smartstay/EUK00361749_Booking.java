package smartstay;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A room reservation made by a guest.
 *
 * <p>Bookings are immutable. Cancelling or modifying a booking creates a new object with the same
 * booking ID, which replaces the old one in the store. Objects are created through the nested
 * {@link EUK00361749_BookingBuilder} (Builder design pattern) because a booking has several
 * fields and a readable, step-by-step construction is safer than a long constructor call.
 */
public final class EUK00361749_Booking {

    private final String bookingId;
    private final EUK00361749_Guest guest;
    private final EUK00361749_Room room;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final int numberOfGuests;
    private final boolean cancelled;

    /** Private constructor: bookings can only be created through the builder. */
    private EUK00361749_Booking(EUK00361749_BookingBuilder builder) {
        this.bookingId = builder.bookingId;
        this.guest = builder.guest;
        this.room = builder.room;
        this.checkInDate = builder.checkInDate;
        this.checkOutDate = builder.checkOutDate;
        this.numberOfGuests = builder.numberOfGuests;
        this.cancelled = builder.cancelled;
    }

    public String getBookingId() {
        return bookingId;
    }

    public EUK00361749_Guest getGuest() {
        return guest;
    }

    public EUK00361749_Room getRoom() {
        return room;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /** @return number of nights between check-in and check-out. */
    public long getNumberOfNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    /**
     * Interval-overlap check used by the availability search.
     *
     * <p>Stays are half-open intervals [checkIn, checkOut), so a guest checking out on the same day
     * another checks in does not clash. Two intervals A and B overlap when
     * A.start &lt; B.end AND B.start &lt; A.end. Cancelled bookings never overlap.
     */
    public boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return !cancelled
                && checkInDate.isBefore(otherCheckOut)
                && otherCheckIn.isBefore(checkOutDate);
    }

    /** @return a cancelled copy of this booking with the same ID. */
    public EUK00361749_Booking cancel() {
        return toBuilder().cancelled(true).build();
    }

    /** @return a builder pre-filled with this booking's values, used for modifications. */
    public EUK00361749_BookingBuilder toBuilder() {
        return new EUK00361749_BookingBuilder(bookingId, guest, room)
                .stayDates(checkInDate, checkOutDate)
                .numberOfGuests(numberOfGuests)
                .cancelled(cancelled);
    }

    @Override
    public String toString() {
        return String.format("%-7s | %-22s | Room %-6s %-12s | %s to %s | %d night(s) | %d guest(s) | %s",
                bookingId, guest.getFullName(), room.getRoomId(), room.getRoomType(),
                checkInDate, checkOutDate, getNumberOfNights(), numberOfGuests,
                cancelled ? "CANCELLED" : "CONFIRMED");
    }

    /**
     * Builder for {@link EUK00361749_Booking} (Builder design pattern, as in the module notes).
     * Required values go in the constructor; the remaining values are set with chained methods and
     * everything is validated once in {@link #build()}.
     */
    public static final class EUK00361749_BookingBuilder {

        private final String bookingId;
        private final EUK00361749_Guest guest;
        private final EUK00361749_Room room;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private int numberOfGuests = 1;
        private boolean cancelled = false;

        /** Required fields: who is staying, in which room, under which booking ID. */
        public EUK00361749_BookingBuilder(String bookingId, EUK00361749_Guest guest, EUK00361749_Room room) {
            this.bookingId = bookingId;
            this.guest = guest;
            this.room = room;
        }

        public EUK00361749_BookingBuilder stayDates(LocalDate checkIn, LocalDate checkOut) {
            this.checkInDate = checkIn;
            this.checkOutDate = checkOut;
            return this;
        }

        public EUK00361749_BookingBuilder numberOfGuests(int count) {
            this.numberOfGuests = count;
            return this;
        }

        public EUK00361749_BookingBuilder cancelled(boolean isCancelled) {
            this.cancelled = isCancelled;
            return this;
        }

        /**
         * Validates the collected values and creates the booking.
         *
         * @throws IllegalArgumentException if a value breaks a booking rule
         */
        public EUK00361749_Booking build() {
            if (bookingId == null || bookingId.isBlank()) {
                throw new IllegalArgumentException("Booking ID is required.");
            }
            Objects.requireNonNull(guest, "Guest is required.");
            Objects.requireNonNull(room, "Room is required.");
            if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
                throw new IllegalArgumentException("Check-out date must be after the check-in date.");
            }
            int maximumGuests = room.getRoomType().getMaximumGuests();
            if (numberOfGuests < 1 || numberOfGuests > maximumGuests) {
                throw new IllegalArgumentException("Number of guests for a " + room.getRoomType()
                        + " room must be between 1 and " + maximumGuests + ".");
            }
            return new EUK00361749_Booking(this);
        }
    }
}
