package smartstay;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Command-line user interface and program entry point.
 *
 * <p>This class only handles input and output. Every business rule is delegated to
 * {@link EUK00361749_Service}. Usability features: numbered menus that stay open until the user
 * chooses Back, inputs that are re-asked when invalid (instead of aborting the whole action), a
 * price preview before a booking is confirmed, and short sequential booking IDs (BK0001).
 */
public final class EUK00361749_Cli {

    private static final String DIVIDER = "-".repeat(70);

    private final Scanner input = new Scanner(System.in);
    private final EUK00361749_HotelSystem hotelSystem;

    private EUK00361749_Cli(EUK00361749_HotelSystem hotelSystem) {
        this.hotelSystem = hotelSystem;
    }

    private EUK00361749_Service service() {
        return hotelSystem.getService();
    }

    // =====================================================================
    // Menus
    // =====================================================================

    private void run() {
        printHeading("SMARTSTAY HOTEL BOOKING SYSTEM");
        while (true) {
            System.out.println("\nMAIN MENU");
            System.out.println("  1. Administrator login");
            System.out.println("  2. Guest services");
            System.out.println("  0. Exit");
            int choice = readInt("Choose an option", 0, 2);
            switch (choice) {
                case 1 -> administratorLogin();
                case 2 -> guestMenu();
                default -> {
                    System.out.println("Goodbye. All changes have been saved.");
                    return;
                }
            }
        }
    }

    /** Allows three login attempts before returning to the main menu. */
    private void administratorLogin() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String username = readText("Username");
            String password = readText("Password");
            EUK00361749_Admin admin = hotelSystem.loginAdministrator(username, password);
            if (admin != null) {
                System.out.println("Welcome, " + admin.getFullName() + ".");
                administratorMenu();
                return;
            }
            System.out.println("Incorrect username or password (attempt " + attempt + " of 3).");
        }
    }

    private void administratorMenu() {
        while (true) {
            printHeading("ADMINISTRATOR MENU");
            System.out.println("  1. Add room");
            System.out.println("  2. Update room details");
            System.out.println("  3. Change room availability status");
            System.out.println("  4. Remove room");
            System.out.println("  5. View all rooms");
            System.out.println("  6. View registered guests");
            System.out.println("  0. Log out");
            int choice = readInt("Choose an option", 0, 6);
            if (choice == 0) {
                return;
            }
            runSafely(() -> {
                switch (choice) {
                    case 1 -> addRoom();
                    case 2 -> updateRoom();
                    case 3 -> changeRoomStatus();
                    case 4 -> removeRoom();
                    case 5 -> printRooms(service().getAllRooms());
                    case 6 -> printGuests(service().getAllGuests());
                    default -> { }
                }
            });
        }
    }

    private void guestMenu() {
        while (true) {
            printHeading("GUEST MENU");
            System.out.println("  1. Register as a guest");
            System.out.println("  2. Search available rooms");
            System.out.println("  3. Make a reservation");
            System.out.println("  4. Search reservations (guest name or booking ID)");
            System.out.println("  5. View reservations by check-in date");
            System.out.println("  6. Cancel a reservation");
            System.out.println("  7. Modify a reservation");
            System.out.println("  8. View invoice");
            System.out.println("  9. Export invoice to a text file");
            System.out.println("  0. Back to main menu");
            int choice = readInt("Choose an option", 0, 9);
            if (choice == 0) {
                return;
            }
            runSafely(() -> {
                switch (choice) {
                    case 1 -> registerGuest();
                    case 2 -> searchRooms();
                    case 3 -> makeReservation();
                    case 4 -> printBookings(service().searchReservations(readText("Guest name or booking ID")));
                    case 5 -> viewByCheckInDate();
                    case 6 -> cancelReservation();
                    case 7 -> modifyReservation();
                    case 8 -> System.out.println(service().generateInvoice(readText("Booking ID")));
                    case 9 -> exportInvoice();
                    default -> { }
                }
            });
        }
    }

    // =====================================================================
    // Administrator actions
    // =====================================================================

    private void addRoom() throws IOException {
        String roomId = readText("New room ID (e.g. 101)");
        EUK00361749_RoomType roomType = chooseOption("Room type", EUK00361749_RoomType.values());
        EUK00361749_BedType bedType = chooseOption("Bed type", EUK00361749_BedType.values());
        EUK00361749_RoomStatus status = chooseOption("Availability status", EUK00361749_RoomStatus.values());
        service().addRoom(new EUK00361749_Room(roomId, roomType, bedType, status));
        hotelSystem.saveChanges();
        System.out.println("Room " + roomId.toUpperCase() + " added.");
    }

    private void updateRoom() throws IOException {
        String roomId = readText("Room ID to update");
        EUK00361749_RoomType roomType = chooseOption("New room type", EUK00361749_RoomType.values());
        EUK00361749_BedType bedType = chooseOption("New bed type", EUK00361749_BedType.values());
        EUK00361749_RoomStatus status = chooseOption("New availability status", EUK00361749_RoomStatus.values());
        service().updateRoom(new EUK00361749_Room(roomId, roomType, bedType, status));
        hotelSystem.saveChanges();
        System.out.println("Room " + roomId.toUpperCase() + " updated.");
    }

    private void changeRoomStatus() throws IOException {
        String roomId = readText("Room ID");
        EUK00361749_RoomStatus status = chooseOption("New availability status", EUK00361749_RoomStatus.values());
        EUK00361749_Room updated = service().changeRoomStatus(roomId, status);
        hotelSystem.saveChanges();
        System.out.println("Room " + updated.getRoomId() + " is now " + updated.getStatus() + ".");
    }

    private void removeRoom() throws IOException {
        String roomId = readText("Room ID to remove");
        if (confirm("Permanently remove room " + roomId.toUpperCase() + "?")) {
            service().removeRoom(roomId);
            hotelSystem.saveChanges();
            System.out.println("Room removed.");
        } else {
            System.out.println("Nothing was removed.");
        }
    }

    // =====================================================================
    // Guest actions
    // =====================================================================

    private void registerGuest() throws IOException {
        EUK00361749_Guest guest = new EUK00361749_Guest(
                readText("NIC or passport number"),
                readText("Full name"),
                readText("Contact number"),
                readText("Email address"));
        service().registerGuest(guest);
        hotelSystem.saveChanges();
        System.out.println("Registration complete. Use NIC/passport " + guest.getNicOrPassport() + " when booking.");
    }

    private void searchRooms() {
        EUK00361749_RoomType roomType = chooseOption("Room type", EUK00361749_RoomType.values());
        LocalDate checkIn = readDate("Check-in date");
        LocalDate checkOut = readDate("Check-out date");
        printRooms(service().searchAvailableRooms(roomType, checkIn, checkOut));
    }

    /**
     * Booking flow required by the brief: room type, number of guests, check-in and check-out.
     * The guest then picks one of the free rooms, sees the full price and confirms the advance.
     */
    private void makeReservation() throws IOException {
        EUK00361749_Guest guest = requireRegisteredGuest();
        EUK00361749_RoomType roomType = chooseOption("Room type", EUK00361749_RoomType.values());
        int numberOfGuests = readInt("Number of guests", 1, roomType.getMaximumGuests());
        LocalDate checkIn = readDate("Check-in date");
        LocalDate checkOut = readDate("Check-out date");

        List<EUK00361749_Room> freeRooms = service().searchAvailableRooms(roomType, checkIn, checkOut);
        if (freeRooms.isEmpty()) {
            System.out.println("Sorry, no " + roomType + " rooms are free for those dates.");
            return;
        }
        EUK00361749_Room room = chooseOption("Choose a room", freeRooms.toArray(new EUK00361749_Room[0]));

        System.out.println("\nPRICE PREVIEW");
        System.out.println(service().quoteReservation(guest.getNicOrPassport(), room.getRoomId(),
                numberOfGuests, checkIn, checkOut));
        if (!confirm("A refundable advance of " + EUK00361749_Invoice.formatMoney(EUK00361749_Invoice.BOOKING_ADVANCE)
                + " is charged now. Confirm the reservation?")) {
            System.out.println("Reservation not made.");
            return;
        }
        EUK00361749_Booking booking = service().makeReservation(guest.getNicOrPassport(), room.getRoomId(),
                numberOfGuests, checkIn, checkOut);
        hotelSystem.saveChanges();
        System.out.println("Reservation confirmed. Your booking ID is " + booking.getBookingId() + ".");
    }

    private void viewByCheckInDate() {
        String answer = readText("Check-in date (YYYY-MM-DD) or ALL");
        LocalDate date = answer.equalsIgnoreCase("ALL") ? null : parseDate(answer);
        printBookings(service().viewReservationsByCheckIn(date));
    }

    private void cancelReservation() throws IOException {
        EUK00361749_Booking booking = service().findBookingOrFail(readText("Booking ID"));
        System.out.println(booking);
        if (confirm("Cancel this reservation?")) {
            service().cancelReservation(booking.getBookingId());
            hotelSystem.saveChanges();
            System.out.println("Reservation cancelled. Refund due: "
                    + EUK00361749_Invoice.formatMoney(EUK00361749_Invoice.BOOKING_ADVANCE) + ".");
        }
    }

    private void modifyReservation() throws IOException {
        EUK00361749_Booking booking = service().findBookingOrFail(readText("Booking ID"));
        System.out.println("Current: " + booking);
        String newRoomId = readText("Room ID (press Enter to keep " + booking.getRoom().getRoomId() + ")", true);
        if (newRoomId.isEmpty()) {
            newRoomId = booking.getRoom().getRoomId();
        }
        int numberOfGuests = readInt("Number of guests", 1, EUK00361749_RoomType.FAMILY_SUITE.getMaximumGuests());
        LocalDate checkIn = readDate("New check-in date");
        LocalDate checkOut = readDate("New check-out date");
        EUK00361749_Booking revised = service().modifyReservation(booking.getBookingId(), newRoomId,
                numberOfGuests, checkIn, checkOut);
        hotelSystem.saveChanges();
        System.out.println("Reservation updated: " + revised);
    }

    private void exportInvoice() throws IOException {
        EUK00361749_Invoice invoice = service().generateInvoice(readText("Booking ID"));
        Path folder = hotelSystem.getDataFile().toAbsolutePath().getParent().resolve("invoices");
        Files.createDirectories(folder);
        Path file = folder.resolve("invoice-" + invoice.getBooking().getBookingId() + ".txt");
        Files.writeString(file, invoice.toString());
        System.out.println("Invoice saved to " + file);
    }

    /** Asks for a NIC/passport until a registered one is entered (or the user gives up). */
    private EUK00361749_Guest requireRegisteredGuest() {
        EUK00361749_Guest guest = service().findGuest(readText("Your NIC or passport number"));
        if (guest == null) {
            throw new IllegalArgumentException("This NIC/passport is not registered. Choose option 1 to register.");
        }
        System.out.println("Booking for " + guest.getFullName() + ".");
        return guest;
    }

    // =====================================================================
    // Output helpers
    // =====================================================================

    private static void printHeading(String title) {
        System.out.println("\n" + DIVIDER);
        System.out.println("  " + title);
        System.out.println(DIVIDER);
    }

    private static void printRooms(List<EUK00361749_Room> rooms) {
        if (rooms.isEmpty()) {
            System.out.println("No rooms found.");
            return;
        }
        System.out.printf("%-8s | %-12s | %-6s | %14s | %-14s | %-9s | %s%n",
                "Room", "Type", "Bed", "Daily rate", "Capacity", "Breakfast", "Status");
        rooms.forEach(System.out::println);
    }

    private static void printGuests(List<EUK00361749_Guest> guests) {
        if (guests.isEmpty()) {
            System.out.println("No guests registered.");
            return;
        }
        guests.forEach(System.out::println);
    }

    private static void printBookings(List<EUK00361749_Booking> bookings) {
        if (bookings.isEmpty()) {
            System.out.println("No reservations found.");
            return;
        }
        bookings.forEach(System.out::println);
    }

    /** Runs one menu action and turns rule violations into a friendly message. */
    private void runSafely(MenuAction action) {
        try {
            action.run();
        } catch (IllegalArgumentException | NullPointerException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Storage error: " + ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface MenuAction {
        void run() throws IOException;
    }

    // =====================================================================
    // Input helpers (re-ask until valid)
    // =====================================================================

    private String readText(String prompt) {
        return readText(prompt, false);
    }

    private String readText(String prompt, boolean allowBlank) {
        while (true) {
            System.out.print(prompt + ": ");
            String value = input.nextLine().trim();
            if (!value.isEmpty() || allowBlank) {
                return value;
            }
            System.out.println("  This field cannot be empty.");
        }
    }

    private int readInt(String prompt, int minimum, int maximum) {
        while (true) {
            String value = readText(prompt + " (" + minimum + "-" + maximum + ")");
            try {
                int number = Integer.parseInt(value);
                if (number >= minimum && number <= maximum) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("  Please enter a whole number from " + minimum + " to " + maximum + ".");
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            try {
                return parseDate(readText(prompt + " (YYYY-MM-DD)"));
            } catch (IllegalArgumentException ex) {
                System.out.println("  " + ex.getMessage());
            }
        }
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Dates must use the format YYYY-MM-DD, e.g. 2026-12-24.");
        }
    }

    /** Shows a numbered list and returns the chosen item. */
    private <T> T chooseOption(String prompt, T[] options) {
        System.out.println(prompt + ":");
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i]);
        }
        return options[readInt("Select", 1, options.length) - 1];
    }

    private boolean confirm(String question) {
        while (true) {
            String answer = readText(question + " (Y/N)");
            if (answer.equalsIgnoreCase("Y") || answer.equalsIgnoreCase("YES")) {
                return true;
            }
            if (answer.equalsIgnoreCase("N") || answer.equalsIgnoreCase("NO")) {
                return false;
            }
            System.out.println("  Please answer Y or N.");
        }
    }

    // =====================================================================
    // Entry point
    // =====================================================================

    /**
     * Starts SmartStay. An optional argument sets the data file (default data/smartstay.db).
     * A file lock stops two copies of the program writing to the same file at once.
     */
    public static void main(String[] args) {
        Path dataFile = Path.of(args.length == 0 ? "data/smartstay.db" : args[0]).toAbsolutePath();
        try {
            Files.createDirectories(dataFile.getParent());
            Path lockFile = dataFile.resolveSibling(dataFile.getFileName() + ".lock");
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = channel.tryLock()) {
                if (lock == null) {
                    throw new IOException("SmartStay is already running with this data file.");
                }
                new EUK00361749_Cli(EUK00361749_HotelSystem.getInstance(dataFile)).run();
            }
        } catch (NoSuchElementException ex) {
            System.out.println("\nInput ended. All saved records are kept.");
        } catch (IOException | OverlappingFileLockException ex) {
            System.err.println("SmartStay stopped safely: " + ex.getMessage());
            System.exit(1);
        }
    }
}
