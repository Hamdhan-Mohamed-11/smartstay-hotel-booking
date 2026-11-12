package smartstay;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Immutable booking snapshot; changes replace the object under the same ID. */
public final class EUK00361749_Booking {
  private final String id;
  private final EUK00361749_Guest guest;
  private final EUK00361749_Room room;
  private final LocalDate checkIn, checkOut;
  private final int guests;
  private final boolean cancelled;

  public EUK00361749_Booking(
      String id,
      EUK00361749_Guest guest,
      EUK00361749_Room room,
      LocalDate in,
      LocalDate out,
      int guests,
      boolean cancelled) {
    if (id == null || id.isBlank()) throw new IllegalArgumentException("Booking ID required.");
    this.guest = Objects.requireNonNull(guest);
    this.room = Objects.requireNonNull(room);
    if (in == null || out == null || !out.isAfter(in))
      throw new IllegalArgumentException("Checkout must follow check-in.");
    if (guests < 1 || guests > room.getType().capacity() + 2)
      throw new IllegalArgumentException("Guest count must be 1 to included capacity + 2.");
    this.id = id;
    this.checkIn = in;
    this.checkOut = out;
    this.guests = guests;
    this.cancelled = cancelled;
  }

  public String getId() {
    return id;
  }

  public EUK00361749_Guest getGuest() {
    return guest;
  }

  public EUK00361749_Room getRoom() {
    return room;
  }

  public LocalDate getCheckIn() {
    return checkIn;
  }

  public LocalDate getCheckOut() {
    return checkOut;
  }

  public int getGuests() {
    return guests;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public long nights() {
    return ChronoUnit.DAYS.between(checkIn, checkOut);
  }

  public boolean overlaps(LocalDate in, LocalDate out) {
    return !cancelled && checkIn.isBefore(out) && in.isBefore(checkOut);
  }

  public EUK00361749_Booking cancel() {
    return new EUK00361749_Booking(id, guest, room, checkIn, checkOut, guests, true);
  }

  @Override
  public String toString() {
    return id
        + " | "
        + guest.getName()
        + " | "
        + room.getId()
        + " | "
        + checkIn
        + " to "
        + checkOut
        + " | guests "
        + guests
        + " | "
        + (cancelled ? "CANCELLED" : "CONFIRMED");
  }
}
