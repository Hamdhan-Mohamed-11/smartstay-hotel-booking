package smartstay;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-memory collections of rooms, guests and bookings, plus saving to and loading from a file.
 *
 * <p>Data structure choice: each collection is a {@link LinkedHashMap} keyed by its unique ID.
 * Looking up a room, guest or booking by ID is O(1) on average (needed for every reservation and
 * duplicate check), and the linked order keeps listings in insertion order. Getters return copies
 * so callers cannot change the internal maps (encapsulation).
 */
public final class EUK00361749_Store {

    private static final String FILE_HEADER = "SMARTSTAY-1";

    private final Map<String, EUK00361749_Room> roomsById = new LinkedHashMap<>();
    private final Map<String, EUK00361749_Guest> guestsByDocument = new LinkedHashMap<>();
    private final Map<String, EUK00361749_Booking> bookingsById = new LinkedHashMap<>();

    // ---------- read access (returns copies) ----------

    public List<EUK00361749_Room> getAllRooms() {
        return new ArrayList<>(roomsById.values());
    }

    public List<EUK00361749_Guest> getAllGuests() {
        return new ArrayList<>(guestsByDocument.values());
    }

    public List<EUK00361749_Booking> getAllBookings() {
        return new ArrayList<>(bookingsById.values());
    }

    /** @return the room, or null if no room has this ID. */
    public EUK00361749_Room findRoom(String roomId) {
        return roomsById.get(roomId.trim().toUpperCase(Locale.ROOT));
    }

    /** @return the guest, or null if the NIC/passport is not registered. */
    public EUK00361749_Guest findGuest(String nicOrPassport) {
        return guestsByDocument.get(nicOrPassport.trim().toUpperCase(Locale.ROOT));
    }

    /** @return the booking, or null if the ID is unknown. */
    public EUK00361749_Booking findBooking(String bookingId) {
        return bookingsById.get(bookingId.trim().toUpperCase(Locale.ROOT));
    }

    // ---------- write access (package-private: only the service changes data) ----------

    void saveRoom(EUK00361749_Room room) {
        roomsById.put(room.getRoomId(), room);
    }

    void saveGuest(EUK00361749_Guest guest) {
        guestsByDocument.put(guest.getNicOrPassport(), guest);
    }

    void saveBooking(EUK00361749_Booking booking) {
        bookingsById.put(booking.getBookingId().toUpperCase(Locale.ROOT), booking);
    }

    void deleteRoom(String roomId) {
        roomsById.remove(roomId);
    }

    // ---------- file persistence ----------

    /**
     * Writes all records to a text file. Each field is Base64-encoded so that tabs or new lines in
     * user input cannot break the file format (this is encoding, not encryption). The data is
     * written to a temporary file first and then moved over the old file, so a crash part-way
     * through never leaves a half-written data file.
     */
    public void saveToFile(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(FILE_HEADER);
        for (EUK00361749_Room room : roomsById.values()) {
            lines.add(encodeRow("R", room.getRoomId(), room.getRoomType().name(),
                    room.getBedType().name(), room.getStatus().name()));
        }
        for (EUK00361749_Guest guest : guestsByDocument.values()) {
            lines.add(encodeRow("G", guest.getNicOrPassport(), guest.getFullName(),
                    guest.getContactNumber(), guest.getEmail()));
        }
        for (EUK00361749_Booking booking : bookingsById.values()) {
            EUK00361749_Room room = booking.getRoom();
            lines.add(encodeRow("B", booking.getBookingId(), booking.getGuest().getNicOrPassport(),
                    room.getRoomId(), room.getRoomType().name(), room.getBedType().name(),
                    room.getStatus().name(), booking.getCheckInDate().toString(),
                    booking.getCheckOutDate().toString(), String.valueOf(booking.getNumberOfGuests()),
                    String.valueOf(booking.isCancelled())));
        }

        Path target = file.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path tempFile = Files.createTempFile(target.getParent(), "smartstay-", ".tmp");
        try {
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Reads a data file. A missing file gives an empty store (first run). A damaged file is
     * rejected with an IOException and left untouched, so data is never silently overwritten.
     */
    public static EUK00361749_Store loadFromFile(Path file) throws IOException {
        EUK00361749_Store store = new EUK00361749_Store();
        if (!Files.exists(file)) {
            return store;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(FILE_HEADER)) {
            throw new IOException("Unknown or damaged data file.");
        }
        try {
            for (String line : lines.subList(1, lines.size())) {
                String[] fields = decodeRow(line);
                switch (fields[0]) {
                    case "R" -> store.loadRoom(fields);
                    case "G" -> store.loadGuest(fields);
                    case "B" -> store.loadBooking(fields);
                    default -> throw new IllegalArgumentException("Unknown record type.");
                }
            }
            return store;
        } catch (RuntimeException ex) {
            throw new IOException("Invalid data file; the original file was not changed.", ex);
        }
    }

    private void loadRoom(String[] fields) {
        if (fields.length != 5 || findRoom(fields[1]) != null) {
            throw new IllegalArgumentException("Bad room record.");
        }
        saveRoom(new EUK00361749_Room(fields[1], EUK00361749_RoomType.valueOf(fields[2]),
                EUK00361749_BedType.valueOf(fields[3]), EUK00361749_RoomStatus.valueOf(fields[4])));
    }

    private void loadGuest(String[] fields) {
        if (fields.length != 5 || findGuest(fields[1]) != null) {
            throw new IllegalArgumentException("Bad guest record.");
        }
        saveGuest(new EUK00361749_Guest(fields[1], fields[2], fields[3], fields[4]));
    }

    private void loadBooking(String[] fields) {
        boolean validFlag = fields.length == 11 && (fields[10].equals("true") || fields[10].equals("false"));
        if (!validFlag || findBooking(fields[1]) != null || findRoom(fields[3]) == null
                || findGuest(fields[2]) == null) {
            throw new IllegalArgumentException("Bad booking record.");
        }
        // The booking keeps the room details that applied when it was made (a snapshot).
        EUK00361749_Room roomSnapshot = new EUK00361749_Room(fields[3], EUK00361749_RoomType.valueOf(fields[4]),
                EUK00361749_BedType.valueOf(fields[5]), EUK00361749_RoomStatus.valueOf(fields[6]));
        saveBooking(new EUK00361749_Booking.EUK00361749_BookingBuilder(fields[1], findGuest(fields[2]), roomSnapshot)
                .stayDates(LocalDate.parse(fields[7]), LocalDate.parse(fields[8]))
                .numberOfGuests(Integer.parseInt(fields[9]))
                .cancelled(Boolean.parseBoolean(fields[10]))
                .build());
    }

    private static String encodeRow(String... fields) {
        String[] encoded = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            encoded[i] = Base64.getEncoder().encodeToString(fields[i].getBytes(StandardCharsets.UTF_8));
        }
        return String.join("\t", encoded);
    }

    private static String[] decodeRow(String line) {
        return Arrays.stream(line.split("\t", -1))
                .map(field -> new String(Base64.getDecoder().decode(field), StandardCharsets.UTF_8))
                .toArray(String[]::new);
    }
}
