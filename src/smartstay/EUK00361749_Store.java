package smartstay;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Single versioned snapshot file. Base64 fields prevent delimiter collisions. */
public final class EUK00361749_Store {
  private final Map<String, EUK00361749_Room> rooms = new LinkedHashMap<>();
  private final Map<String, EUK00361749_Guest> guests = new LinkedHashMap<>();
  private final Map<String, EUK00361749_Booking> bookings = new LinkedHashMap<>();

  public List<EUK00361749_Room> rooms() {
    return new ArrayList<>(rooms.values());
  }

  public List<EUK00361749_Guest> guests() {
    return new ArrayList<>(guests.values());
  }

  public List<EUK00361749_Booking> bookings() {
    return new ArrayList<>(bookings.values());
  }

  public EUK00361749_Room room(String id) {
    return rooms.get(id.toUpperCase(Locale.ROOT));
  }

  public EUK00361749_Guest guest(String id) {
    return guests.get(id.toUpperCase(Locale.ROOT));
  }

  public EUK00361749_Booking booking(String id) {
    return bookings.get(id);
  }

  void put(EUK00361749_Room r) {
    rooms.put(r.getId(), r);
  }

  void put(EUK00361749_Guest g) {
    guests.put(g.getDocument(), g);
  }

  void put(EUK00361749_Booking b) {
    bookings.put(b.getId(), b);
  }

  void removeRoom(String id) {
    rooms.remove(id);
  }

  private static String encode(String s) {
    return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
  }

  private static String decode(String s) {
    return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
  }

  private static String row(String... fields) {
    return String.join(
        "\t", Arrays.stream(fields).map(EUK00361749_Store::encode).toArray(String[]::new));
  }

  public void save(Path file) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("SMARTSTAY-1");
    for (EUK00361749_Room r : rooms())
      lines.add(row("R", r.getId(), r.getType().name(), r.getBed().name(), r.getStatus().name()));
    for (EUK00361749_Guest g : guests())
      lines.add(row("G", g.getDocument(), g.getName(), g.getPhone(), g.getEmail()));
    for (EUK00361749_Booking b : bookings())
      lines.add(
          row(
              "B",
              b.getId(),
              b.getGuest().getDocument(),
              b.getRoom().getId(),
              b.getRoom().getType().name(),
              b.getRoom().getBed().name(),
              b.getRoom().getStatus().name(),
              b.getCheckIn().toString(),
              b.getCheckOut().toString(),
              "" + b.getGuests(),
              "" + b.isCancelled()));
    Path absolute = file.toAbsolutePath();
    Files.createDirectories(absolute.getParent());
    Path temp = Files.createTempFile(absolute.getParent(), "smartstay-", ".tmp");
    try {
      Files.write(temp, lines, StandardCharsets.UTF_8);
      try {
        Files.move(
            temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  public static EUK00361749_Store load(Path file) throws IOException {
    EUK00361749_Store store = new EUK00361749_Store();
    if (!Files.exists(file)) return store;
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    if (lines.isEmpty() || !lines.get(0).equals("SMARTSTAY-1"))
      throw new IOException("Unknown or damaged data file.");
    try {
      for (String line : lines.subList(1, lines.size())) {
        String[] f =
            Arrays.stream(line.split("\t", -1))
                .map(EUK00361749_Store::decode)
                .toArray(String[]::new);
        switch (f[0]) {
          case "R" -> {
            if (f.length != 5 || store.room(f[1]) != null) throw new IllegalArgumentException();
            store.put(
                new EUK00361749_Room(
                    f[1],
                    EUK00361749_RoomType.valueOf(f[2]),
                    EUK00361749_BedType.valueOf(f[3]),
                    EUK00361749_RoomStatus.valueOf(f[4])));
          }
          case "G" -> {
            if (f.length != 5 || store.guest(f[1]) != null) throw new IllegalArgumentException();
            store.put(new EUK00361749_Guest(f[1], f[2], f[3], f[4]));
          }
          case "B" -> {
            if (f.length != 11
                || store.booking(f[1]) != null
                || store.room(f[3]) == null
                || !(f[10].equals("true") || f[10].equals("false")))
              throw new IllegalArgumentException();
            EUK00361749_Room snapshot =
                new EUK00361749_Room(
                    f[3],
                    EUK00361749_RoomType.valueOf(f[4]),
                    EUK00361749_BedType.valueOf(f[5]),
                    EUK00361749_RoomStatus.valueOf(f[6]));
            store.put(
                new EUK00361749_Booking(
                    f[1],
                    store.guest(f[2]),
                    snapshot,
                    LocalDate.parse(f[7]),
                    LocalDate.parse(f[8]),
                    Integer.parseInt(f[9]),
                    Boolean.parseBoolean(f[10])));
          }
          default -> throw new IllegalArgumentException();
        }
      }
      return store;
    } catch (RuntimeException ex) {
      throw new IOException("Invalid data file; original file preserved.", ex);
    }
  }
}
