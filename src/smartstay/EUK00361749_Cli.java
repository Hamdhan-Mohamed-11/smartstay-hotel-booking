package smartstay;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Terminal controller. Saves after every successful mutation. */
public final class EUK00361749_Cli {
  private final Scanner input = new Scanner(System.in);
  private EUK00361749_Store store;
  private EUK00361749_Service service;
  private final Path file;

  private EUK00361749_Cli(Path file) throws IOException {
    this.file = file;
    reload();
  }

  private void reload() throws IOException {
    store = EUK00361749_Store.load(file);
    service =
        new EUK00361749_Service(
            store, Clock.system(EUK00361749_Service.ZONE), new EUK00361749_LongStayDiscount());
  }

  private String ask(String prompt) {
    System.out.print(prompt + ": ");
    return input.nextLine().trim();
  }

  private LocalDate date(String prompt) {
    return LocalDate.parse(ask(prompt + " (YYYY-MM-DD)"));
  }

  private int number(String prompt) {
    return Integer.parseInt(ask(prompt));
  }

  private <T extends Enum<T>> T choice(String prompt, Class<T> kind) {
    return Enum.valueOf(
        kind,
        ask(prompt + " " + Arrays.toString(kind.getEnumConstants()))
            .toUpperCase(Locale.ROOT)
            .replace(' ', '_'));
  }

  private EUK00361749_Room roomInput() {
    return new EUK00361749_Room(
        ask("Room ID"),
        choice("Room type", EUK00361749_RoomType.class),
        choice("Bed type", EUK00361749_BedType.class),
        choice("Manual status", EUK00361749_RoomStatus.class));
  }

  private static void display(List<?> rows) {
    if (rows.isEmpty()) System.out.println("No matching records.");
    else rows.forEach(System.out::println);
  }

  private void save() throws IOException {
    try {
      store.save(file);
    } catch (IOException ex) {
      reload();
      throw new IOException("Save failed; change rolled back. " + ex.getMessage(), ex);
    }
  }

  private void run() throws IOException {
    System.out.println("SMARTSTAY | Java CLI | local single-user demonstration");
    System.out.println(
        "Role menus demonstrate workflows; no password authentication is implemented.");
    while (true) {
      System.out.println("\n1 Administrator  2 Guest  0 Exit");
      String role = ask("Choose role");
      if (role.equals("0")) return;
      try {
        if (role.equals("1")) admin();
        else if (role.equals("2")) guest();
        else System.out.println("Choose 0, 1 or 2.");
      } catch (IllegalArgumentException | DateTimeException ex) {
        System.out.println("Input rejected: " + ex.getMessage());
      }
    }
  }

  private void admin() throws IOException {
    System.out.println(
        "1 Add room  2 Update room/status  3 Remove room  4 List rooms  5 List guests  0 Back");
    switch (ask("Action")) {
      case "1" -> {
        service.addRoom(roomInput());
        save();
        System.out.println("Room added and saved.");
      }
      case "2" -> {
        service.updateRoom(roomInput());
        save();
        System.out.println("Room updated and saved.");
      }
      case "3" -> {
        service.removeRoom(ask("Room ID"));
        save();
        System.out.println("Room removed and saved.");
      }
      case "4" -> display(service.rooms());
      case "5" -> display(service.guests());
      case "0" -> {}
      default -> System.out.println("Unknown action.");
    }
  }

  private void guest() throws IOException {
    System.out.println("1 Register  2 Search rooms  3 Book  4 Search bookings  5 View by check-in");
    System.out.println("6 Cancel  7 Modify  8 Invoice  9 Export invoice  0 Back");
    switch (ask("Action")) {
      case "1" -> {
        service.register(
            new EUK00361749_Guest(
                ask("NIC/passport"), ask("Full name"), ask("Phone"), ask("Email")));
        save();
        System.out.println("Guest registered and saved.");
      }
      case "2" ->
          display(
              service.searchRooms(
                  choice("Room type", EUK00361749_RoomType.class),
                  date("Check-in"),
                  date("Check-out")));
      case "3" -> {
        String g = ask("Registered NIC/passport"), r = ask("Room ID from search");
        int n = number("Guest count");
        LocalDate in = date("Check-in"), out = date("Check-out");
        if (!ask("Confirm receipt of LKR 10000 advance? YES to record").equalsIgnoreCase("YES")) {
          System.out.println("Booking not created.");
          break;
        }
        EUK00361749_Booking b = service.book(g, r, n, in, out);
        save();
        System.out.println("Reservation saved: " + b);
        System.out.println(service.invoice(b.getId()));
      }
      case "4" -> display(service.searchBookings(ask("Guest name or complete booking ID")));
      case "5" -> {
        String d = ask("Check-in YYYY-MM-DD, or ALL");
        display(service.byCheckIn(d.equalsIgnoreCase("ALL") ? null : LocalDate.parse(d)));
      }
      case "6" -> {
        String id = ask("Booking ID");
        if (ask("Confirm cancellation? YES").equalsIgnoreCase("YES")) {
          service.cancel(id);
          save();
          System.out.println(
              "Cancelled. Refund due: LKR 10000.00 (recorded, not electronically paid).");
        }
      }
      case "7" -> {
        EUK00361749_Booking b =
            service.modify(
                ask("Booking ID"),
                ask("New room ID"),
                number("Guest count"),
                date("Check-in"),
                date("Check-out"));
        save();
        System.out.println("Reservation updated: " + b);
      }
      case "8" -> System.out.println(service.invoice(ask("Booking ID")));
      case "9" -> {
        String id = ask("Booking ID");
        String text = service.invoice(id).toString();
        Path out = file.toAbsolutePath().getParent().resolve("invoices");
        Files.createDirectories(out);
        Files.writeString(out.resolve(id + ".txt"), text);
        System.out.println("Invoice exported to " + out.resolve(id + ".txt"));
      }
      case "0" -> {}
      default -> System.out.println("Unknown action.");
    }
  }

  public static void main(String[] args) {
    Path file = Path.of(args.length == 0 ? "data/smartstay.db" : args[0]).toAbsolutePath();
    try {
      Files.createDirectories(file.getParent());
      try (FileChannel channel =
              FileChannel.open(
                  file.resolveSibling(file.getFileName() + ".lock"),
                  StandardOpenOption.CREATE,
                  StandardOpenOption.WRITE);
          FileLock lock = channel.tryLock()) {
        if (lock == null)
          throw new IOException("Another SmartStay process is using this data file.");
        new EUK00361749_Cli(file).run();
      }
    } catch (NoSuchElementException ex) {
      System.out.println("\nInput ended. Previously saved records retained.");
    } catch (IOException | OverlappingFileLockException ex) {
      System.err.println("Stopped safely: " + ex.getMessage());
      System.exit(1);
    }
  }
}
