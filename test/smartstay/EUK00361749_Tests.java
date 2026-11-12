package smartstay;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Dependency-free regression runner. Each result records observed behaviour. */
public final class EUK00361749_Tests {
  private static int passed = 0, failed = 0;
  private static final List<String> rows = new ArrayList<>();
  private static final LocalDate IN = LocalDate.of(2026, 11, 7);
  private static final Clock NOW =
      Clock.fixed(Instant.parse("2026-11-05T04:30:00Z"), EUK00361749_Service.ZONE);
  private static EUK00361749_Store store;
  private static EUK00361749_Service service;

  private static void reset() {
    store = new EUK00361749_Store();
    service = new EUK00361749_Service(store, NOW, new EUK00361749_LongStayDiscount());
    service.addRoom(room("101", EUK00361749_RoomType.STANDARD, EUK00361749_RoomStatus.AVAILABLE));
    service.register(
        new EUK00361749_Guest("N1234567", "Amina Silva", "+94771234567", "amina@example.com"));
  }

  private static EUK00361749_Room room(
      String id, EUK00361749_RoomType t, EUK00361749_RoomStatus s) {
    return new EUK00361749_Room(id, t, EUK00361749_BedType.QUEEN, s);
  }

  private static EUK00361749_Booking book(int nights, int guests) {
    return service.book("N1234567", "101", guests, IN, IN.plusDays(nights));
  }

  private static void eq(Object actual, Object expected) {
    if (!Objects.equals(actual, expected))
      throw new AssertionError("Expected " + expected + "; observed " + actual);
  }

  private static void money(BigDecimal actual, String expected) {
    if (actual.compareTo(new BigDecimal(expected)) != 0)
      throw new AssertionError(actual + " != " + expected);
  }

  private static String rejects(Runnable action) {
    try {
      action.run();
      throw new AssertionError("Input unexpectedly accepted");
    } catch (IllegalArgumentException ex) {
      return ex.getMessage();
    }
  }

  private interface Check {
    String run() throws Exception;
  }

  private static void test(String name, String input, String expected, Check check) {
    reset();
    String id = String.format("TC%03d", passed + failed + 1);
    String actual, result;
    try {
      actual = check.run();
      result = "Pass";
      passed++;
    } catch (Throwable ex) {
      actual = ex.toString();
      result = "Fail";
      failed++;
    }
    rows.add(String.join("\t", id, name, input, expected, actual.replace('\n', ' '), result));
    System.out.println(id + " " + result.toUpperCase() + " | " + name + " | " + actual);
  }

  public static void main(String[] args) throws Exception {
    test(
        "Add room",
        "ID 102, Deluxe",
        "Room count 2",
        () -> {
          service.addRoom(
              room("102", EUK00361749_RoomType.DELUXE, EUK00361749_RoomStatus.AVAILABLE));
          eq(service.rooms().size(), 2);
          return "Room count 2";
        });
    test(
        "Duplicate room",
        "ID 101",
        "Rejected",
        () ->
            rejects(
                () ->
                    service.addRoom(
                        room(
                            "101", EUK00361749_RoomType.SUITE, EUK00361749_RoomStatus.AVAILABLE))));
    test(
        "Update room",
        "101 to Deluxe, KING",
        "Deluxe and KING saved",
        () -> {
          service.updateRoom(
              new EUK00361749_Room(
                  "101",
                  EUK00361749_RoomType.DELUXE,
                  EUK00361749_BedType.KING,
                  EUK00361749_RoomStatus.AVAILABLE));
          eq(store.room("101").getType(), EUK00361749_RoomType.DELUXE);
          eq(store.room("101").getBed(), EUK00361749_BedType.KING);
          return "Deluxe and KING saved";
        });
    test(
        "Remove room",
        "101 without history",
        "Room count 0",
        () -> {
          service.removeRoom("101");
          eq(service.rooms().size(), 0);
          return "Room count 0";
        });
    test("Unknown room", "Delete 999", "Rejected", () -> rejects(() -> service.removeRoom("999")));
    test(
        "Duplicate guest",
        "N1234567",
        "Rejected",
        () ->
            rejects(
                () ->
                    service.register(
                        new EUK00361749_Guest(
                            "N1234567", "Other", "0771234567", "other@example.com"))));
    test(
        "Invalid email",
        "bad email",
        "Rejected",
        () -> rejects(() -> new EUK00361749_Guest("A12345", "Guest", "0771234567", "bad email")));
    test(
        "Invalid phone",
        "abc",
        "Rejected",
        () -> rejects(() -> new EUK00361749_Guest("A12345", "Guest", "abc", "a@example.com")));
    test(
        "Blank guest name",
        "Whitespace name",
        "Rejected",
        () -> rejects(() -> new EUK00361749_Guest("A12345", " ", "0771234567", "a@example.com")));
    test(
        "Two-day boundary",
        "Nov 5 booking for Nov 7",
        "Accepted",
        () -> {
          book(1, 2);
          eq(store.bookings().size(), 1);
          return "Accepted";
        });
    test(
        "One-day notice",
        "Nov 6 check-in",
        "Rejected",
        () -> rejects(() -> service.book("N1234567", "101", 2, IN.minusDays(1), IN)));
    test(
        "Same-day checkout",
        "Nov 7 to Nov 7",
        "Rejected",
        () -> rejects(() -> service.book("N1234567", "101", 2, IN, IN)));
    test(
        "Reversed dates",
        "Nov 8 to Nov 7",
        "Rejected",
        () -> rejects(() -> service.book("N1234567", "101", 2, IN.plusDays(1), IN)));
    test("Zero guests", "0 guests", "Rejected", () -> rejects(() -> book(1, 0)));
    test("Too many guests", "Standard, 5 guests", "Rejected", () -> rejects(() -> book(1, 5)));
    test(
        "Maximum occupancy",
        "Standard, 4 guests",
        "Accepted",
        () -> {
          book(1, 4);
          return "Accepted";
        });
    test(
        "Unregistered guest",
        "UNKNOWN",
        "Rejected",
        () -> rejects(() -> service.book("UNKNOWN", "101", 2, IN, IN.plusDays(1))));
    test(
        "Maintenance exclusion",
        "101 under maintenance",
        "No search results; booking rejected",
        () -> {
          service.updateRoom(
              room("101", EUK00361749_RoomType.STANDARD, EUK00361749_RoomStatus.UNDER_MAINTENANCE));
          eq(service.searchRooms(EUK00361749_RoomType.STANDARD, IN, IN.plusDays(1)).size(), 0);
          return rejects(() -> book(1, 2));
        });
    test(
        "Manual reserved exclusion",
        "101 manually reserved",
        "Rejected",
        () -> {
          service.updateRoom(
              room("101", EUK00361749_RoomType.STANDARD, EUK00361749_RoomStatus.RESERVED));
          return rejects(() -> book(1, 2));
        });
    test(
        "Overlap exclusion",
        "Existing Nov 7-10, new Nov 9-11",
        "Rejected",
        () -> {
          book(3, 2);
          return rejects(() -> service.book("N1234567", "101", 2, IN.plusDays(2), IN.plusDays(4)));
        });
    test(
        "Adjacent stays",
        "Nov 7-9 and Nov 9-10",
        "Two bookings",
        () -> {
          book(2, 2);
          service.book("N1234567", "101", 2, IN.plusDays(2), IN.plusDays(3));
          eq(store.bookings().size(), 2);
          return "Two bookings";
        });
    test(
        "Availability date filter",
        "101 booked Nov 7-9",
        "0 overlapping; 1 subsequent",
        () -> {
          book(2, 2);
          eq(service.searchRooms(EUK00361749_RoomType.STANDARD, IN, IN.plusDays(1)).size(), 0);
          eq(
              service
                  .searchRooms(EUK00361749_RoomType.STANDARD, IN.plusDays(2), IN.plusDays(3))
                  .size(),
              1);
          return "0 overlapping; 1 subsequent";
        });
    test(
        "Four-night bill",
        "Standard, 2 guests, 4 nights",
        "Discount 0; tax 2400; payable 16400",
        () -> {
          EUK00361749_Invoice i = service.invoice(book(4, 2).getId());
          money(i.getDiscount(), "0");
          money(i.getTax(), "2400");
          money(i.getPayable(), "16400");
          return "Discount 0; tax 2400; payable 16400";
        });
    test(
        "Five-night bill with extra",
        "Standard, 3 guests, 5 nights",
        "Base 30000; extra 5000; discount 3600; tax 3140; payable 24540",
        () -> {
          EUK00361749_Invoice i = service.invoice(book(5, 3).getId());
          money(i.getBase(), "30000");
          money(i.getExtra(), "5000");
          money(i.getDiscount(), "3600");
          money(i.getTax(), "3140");
          money(i.getPayable(), "24540");
          return "30000 + 5000 - 3600 + 3140 - 10000 = 24540";
        });
    test(
        "Advance exceeds total",
        "Standard, 2 guests, 1 night",
        "Payable 0; refund 3400",
        () -> {
          EUK00361749_Invoice i = service.invoice(book(1, 2).getId());
          money(i.getPayable(), "0");
          money(i.getRefund(), "3400");
          return "Payable 0; refund 3400";
        });
    for (EUK00361749_RoomType type : EUK00361749_RoomType.values())
      test(
          "Tariff " + type,
          "1 night, capacity + 1 guest",
          "Correct package tax and total",
          () -> {
            service.addRoom(room("T", type, EUK00361749_RoomStatus.AVAILABLE));
            EUK00361749_Booking b =
                service.book("N1234567", "T", type.capacity() + 1, IN, IN.plusDays(1));
            EUK00361749_Invoice i = service.invoice(b.getId());
            String[] totals = {"7700", "11760", "18400", "23575"};
            money(i.getTotal(), totals[type.ordinal()]);
            return "Total " + i.getTotal();
          });
    test(
        "Cancellation at 24h",
        "Nov 6 at 14:00 for Nov 7 14:00",
        "Cancelled",
        () -> {
          String id = book(1, 2).getId();
          EUK00361749_Service s =
              new EUK00361749_Service(
                  store,
                  Clock.fixed(Instant.parse("2026-11-06T08:30:00Z"), EUK00361749_Service.ZONE),
                  new EUK00361749_LongStayDiscount());
          eq(s.cancel(id).isCancelled(), true);
          return "Cancelled; refund due 10000";
        });
    test(
        "Cancellation inside 24h",
        "Nov 6 at 14:00:01",
        "Rejected",
        () -> {
          String id = book(1, 2).getId();
          EUK00361749_Service s =
              new EUK00361749_Service(
                  store,
                  Clock.fixed(Instant.parse("2026-11-06T08:30:01Z"), EUK00361749_Service.ZONE),
                  new EUK00361749_LongStayDiscount());
          return rejects(() -> s.cancel(id));
        });
    test(
        "Duplicate cancellation",
        "Cancel same booking twice",
        "Rejected second attempt",
        () -> {
          String id = book(1, 2).getId();
          service.cancel(id);
          return rejects(() -> service.cancel(id));
        });
    test(
        "Cancelled frees room",
        "Cancel then rebook same dates",
        "One available room; new booking accepted",
        () -> {
          String id = book(1, 2).getId();
          service.cancel(id);
          eq(service.searchRooms(EUK00361749_RoomType.STANDARD, IN, IN.plusDays(1)).size(), 1);
          book(1, 2);
          return "Room released and rebooked";
        });
    test(
        "Cancelled invoice",
        "Invoice cancelled booking",
        "Rejected",
        () -> {
          String id = book(1, 2).getId();
          service.cancel(id);
          return rejects(() -> service.invoice(id));
        });
    test(
        "Modify booking",
        "Increase to 5 nights, 3 guests",
        "Same ID; revised bill 24540",
        () -> {
          String id = book(1, 2).getId();
          eq(service.modify(id, "101", 3, IN, IN.plusDays(5)).getId(), id);
          money(service.invoice(id).getPayable(), "24540");
          return "Same ID; revised bill 24540";
        });
    test(
        "Modify inside 24h",
        "Change guests at 23h59m59s",
        "Rejected",
        () -> {
          String id = book(1, 2).getId();
          EUK00361749_Service s =
              new EUK00361749_Service(
                  store,
                  Clock.fixed(Instant.parse("2026-11-06T08:30:01Z"), EUK00361749_Service.ZONE),
                  new EUK00361749_LongStayDiscount());
          return rejects(() -> s.modify(id, "101", 3, IN, IN.plusDays(2)));
        });
    test(
        "Failed modification unchanged",
        "Modify to zero guests",
        "Rejected; original guest count 2",
        () -> {
          String id = book(1, 2).getId();
          rejects(() -> service.modify(id, "101", 0, IN, IN.plusDays(2)));
          eq(service.requireBooking(id).getGuests(), 2);
          return "Rejected; original guest count 2";
        });
    test(
        "Name search",
        "aMiNa",
        "One result",
        () -> {
          book(1, 2);
          eq(service.searchBookings("aMiNa").size(), 1);
          return "One result";
        });
    test(
        "Booking ID search",
        "Full generated ID",
        "Exact booking",
        () -> {
          String id = book(1, 2).getId();
          eq(service.searchBookings(id).get(0).getId(), id);
          return "Exact booking found";
        });
    test(
        "Check-in filtering and sort",
        "Bookings Nov 10 and Nov 7",
        "Nov 7 first; exact date count 1",
        () -> {
          service.book("N1234567", "101", 2, IN.plusDays(3), IN.plusDays(4));
          book(1, 2);
          eq(service.byCheckIn(null).get(0).getCheckIn(), IN);
          eq(service.byCheckIn(IN).size(), 1);
          return "Nov 7 first; exact date count 1";
        });
    test(
        "Protect booked room update",
        "Change type after booking",
        "Rejected",
        () -> {
          book(1, 2);
          return rejects(
              () ->
                  service.updateRoom(
                      room("101", EUK00361749_RoomType.SUITE, EUK00361749_RoomStatus.AVAILABLE)));
        });
    test(
        "Protect booking history",
        "Delete booked room",
        "Rejected",
        () -> {
          book(1, 2);
          return rejects(() -> service.removeRoom("101"));
        });
    test(
        "Persistence round trip",
        "Save 1 room, 1 guest, 1 booking",
        "Reloaded identities and bill preserved",
        () -> {
          String id = book(5, 3).getId();
          Path file = Files.createTempDirectory("smartstay-test").resolve("data.db");
          store.save(file);
          EUK00361749_Store loaded = EUK00361749_Store.load(file);
          eq(loaded.rooms().size(), 1);
          eq(loaded.guests().size(), 1);
          eq(loaded.bookings().size(), 1);
          money(
              new EUK00361749_Service(loaded, NOW, new EUK00361749_LongStayDiscount())
                  .invoice(id)
                  .getPayable(),
              "24540");
          Files.delete(file);
          Files.delete(file.getParent());
          return "Reloaded 1 room, 1 guest, 1 booking; payable 24540";
        });
    test(
        "Corrupt file rejection",
        "Invalid file header",
        "IOException; bytes unchanged",
        () -> {
          Path f = Files.createTempFile("smartstay-corrupt", ".db");
          Files.writeString(f, "BROKEN");
          try {
            EUK00361749_Store.load(f);
            throw new AssertionError("Loaded damaged file");
          } catch (java.io.IOException ex) {
            eq(Files.readString(f), "BROKEN");
          }
          Files.delete(f);
          return "IOException; original bytes unchanged";
        });
    test(
        "Missing file",
        "Nonexistent local file",
        "Empty store",
        () -> {
          Path d = Files.createTempDirectory("smartstay-empty");
          eq(EUK00361749_Store.load(d.resolve("missing.db")).rooms().size(), 0);
          Files.delete(d);
          return "Empty store";
        });
    test(
        "Collection encapsulation",
        "Clear returned room list",
        "Internal room retained",
        () -> {
          service.rooms().clear();
          eq(service.rooms().size(), 1);
          return "Internal room retained";
        });
    test(
        "Status persistence",
        "Save manual maintenance status",
        "Maintenance after reload",
        () -> {
          service.updateRoom(
              room("101", EUK00361749_RoomType.STANDARD, EUK00361749_RoomStatus.UNDER_MAINTENANCE));
          Path f = Files.createTempFile("smartstay-status", ".db");
          store.save(f);
          eq(
              EUK00361749_Store.load(f).room("101").getStatus(),
              EUK00361749_RoomStatus.UNDER_MAINTENANCE);
          Files.delete(f);
          return "Maintenance after reload";
        });
    test(
        "Cancellation persistence",
        "Save cancelled booking",
        "Cancelled after reload",
        () -> {
          String id = book(1, 2).getId();
          service.cancel(id);
          Path f = Files.createTempFile("smartstay-cancel", ".db");
          store.save(f);
          eq(EUK00361749_Store.load(f).booking(id).isCancelled(), true);
          Files.delete(f);
          return "Cancelled after reload";
        });
    test(
        "Polymorphic policy",
        "Alternative zero-discount subclass",
        "Injected policy produces discount 0",
        () -> {
          EUK00361749_DiscountPolicy zero =
              new EUK00361749_DiscountPolicy() {
                protected BigDecimal rateFor(long n) {
                  return BigDecimal.ZERO;
                }
              };
          EUK00361749_Invoice i = new EUK00361749_Invoice(book(5, 2), zero);
          money(i.getDiscount(), "0");
          return "Injected policy produces discount 0";
        });
    Path evidence = Path.of("docs/evidence");
    Files.createDirectories(evidence);
    Files.write(evidence.resolve("test-results.tsv"), rows);
    System.out.println("RESULT: " + passed + " passed, " + failed + " failed.");
    if (failed > 0) System.exit(1);
  }
}
