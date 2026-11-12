package smartstay;

import java.time.*;
import java.util.*;

/** Business rules independent of terminal I/O; Clock makes boundaries testable. */
public final class EUK00361749_Service {
  public static final ZoneId ZONE = ZoneId.of("Asia/Colombo");
  private final EUK00361749_Store store;
  private final Clock clock;
  private final EUK00361749_DiscountPolicy discounts;

  public EUK00361749_Service(
      EUK00361749_Store store, Clock clock, EUK00361749_DiscountPolicy discounts) {
    this.store = Objects.requireNonNull(store);
    this.clock = clock.withZone(ZONE);
    this.discounts = Objects.requireNonNull(discounts);
  }

  public void addRoom(EUK00361749_Room room) {
    if (store.room(room.getId()) != null) fail("Room ID already exists.");
    store.put(room);
  }

  public void updateRoom(EUK00361749_Room room) {
    requireRoom(room.getId());
    if (store.bookings().stream()
        .anyMatch(
            b ->
                b.getRoom().getId().equals(room.getId())
                    && !b.isCancelled()
                    && b.getCheckOut().isAfter(LocalDate.now(clock))))
      fail("Cannot change room with current/future bookings.");
    store.put(room);
  }

  public void removeRoom(String id) {
    EUK00361749_Room room = requireRoom(id);
    if (store.bookings().stream().anyMatch(b -> b.getRoom().getId().equals(room.getId())))
      fail("Room has booking history. Retain it and manage its status instead.");
    store.removeRoom(room.getId());
  }

  public void register(EUK00361749_Guest guest) {
    if (store.guest(guest.getDocument()) != null) fail("Guest already registered.");
    store.put(guest);
  }

  public List<EUK00361749_Room> rooms() {
    return store.rooms();
  }

  public List<EUK00361749_Guest> guests() {
    return store.guests();
  }

  private EUK00361749_Room requireRoom(String id) {
    EUK00361749_Room r = store.room(id);
    if (r == null) fail("Room not found.");
    return r;
  }

  public EUK00361749_Booking requireBooking(String id) {
    EUK00361749_Booking b = store.booking(id);
    if (b == null) fail("Booking not found.");
    return b;
  }

  private static void fail(String message) {
    throw new IllegalArgumentException(message);
  }

  private void dates(LocalDate in, LocalDate out) {
    if (in == null || out == null || !out.isAfter(in)) fail("Checkout must follow check-in.");
    if (in.isBefore(LocalDate.now(clock).plusDays(2)))
      fail("Book at least 2 calendar days in advance.");
  }

  private boolean available(EUK00361749_Room room, LocalDate in, LocalDate out, String excluded) {
    return room.getStatus() == EUK00361749_RoomStatus.AVAILABLE
        && store.bookings().stream()
            .noneMatch(
                b ->
                    !b.getId().equals(excluded)
                        && b.getRoom().getId().equals(room.getId())
                        && b.overlaps(in, out));
  }

  public List<EUK00361749_Room> searchRooms(
      EUK00361749_RoomType type, LocalDate in, LocalDate out) {
    dates(in, out);
    return store.rooms().stream()
        .filter(r -> r.getType() == type && available(r, in, out, ""))
        .toList();
  }

  public EUK00361749_Booking book(
      String guestId, String roomId, int count, LocalDate in, LocalDate out) {
    dates(in, out);
    EUK00361749_Guest guest = store.guest(guestId);
    if (guest == null) fail("Register guest first.");
    EUK00361749_Room room = requireRoom(roomId);
    if (!available(room, in, out, "")) fail("Room unavailable for these dates.");
    EUK00361749_Booking booking =
        new EUK00361749_Booking("BK-" + UUID.randomUUID(), guest, room, in, out, count, false);
    store.put(booking);
    return booking;
  }

  private void eligible(EUK00361749_Booking b) {
    if (b.isCancelled()) fail("Booking already cancelled.");
    Instant cutoff =
        b.getCheckIn().atTime(14, 0).atZone(ZONE).toInstant().minus(Duration.ofHours(24));
    if (clock.instant().isAfter(cutoff))
      fail("Changes/cancellation closed within 24 hours of check-in.");
  }

  public EUK00361749_Booking cancel(String id) {
    EUK00361749_Booking b = requireBooking(id);
    eligible(b);
    EUK00361749_Booking c = b.cancel();
    store.put(c);
    return c;
  }

  public EUK00361749_Booking modify(
      String id, String roomId, int count, LocalDate in, LocalDate out) {
    EUK00361749_Booking old = requireBooking(id);
    eligible(old);
    dates(in, out);
    EUK00361749_Room room = requireRoom(roomId);
    if (!available(room, in, out, id)) fail("Room unavailable for these dates.");
    EUK00361749_Booking revised =
        new EUK00361749_Booking(id, old.getGuest(), room, in, out, count, false);
    store.put(revised);
    return revised;
  }

  public List<EUK00361749_Booking> searchBookings(String query) {
    String q = query.trim().toLowerCase(Locale.ROOT);
    if (q.isEmpty()) fail("Enter a name or booking ID.");
    return store.bookings().stream()
        .filter(
            b ->
                b.getId().equalsIgnoreCase(q)
                    || b.getGuest().getName().toLowerCase(Locale.ROOT).contains(q))
        .toList();
  }

  public List<EUK00361749_Booking> byCheckIn(LocalDate date) {
    return store.bookings().stream()
        .filter(b -> date == null || b.getCheckIn().equals(date))
        .sorted(
            Comparator.comparing(EUK00361749_Booking::getCheckIn)
                .thenComparing(EUK00361749_Booking::getId))
        .toList();
  }

  public EUK00361749_Invoice invoice(String id) {
    return new EUK00361749_Invoice(requireBooking(id), discounts);
  }
}
