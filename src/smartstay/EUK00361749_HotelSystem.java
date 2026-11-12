package smartstay;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;

/**
 * The single running SmartStay system (Singleton design pattern).
 *
 * <p>Exactly one object of this class may exist while the program runs, because it owns the one
 * data file and the in-memory records loaded from it. A second instance would hold a separate copy
 * of the data and the two could overwrite each other's changes. The constructor is private and the
 * only way to get the object is {@link #getInstance(Path)}.
 */
public final class EUK00361749_HotelSystem {

    /** Demonstration administrator account (documented in the README). */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private static EUK00361749_HotelSystem instance;

    private final Path dataFile;
    private final EUK00361749_Admin administrator;
    private EUK00361749_Store store;
    private EUK00361749_Service service;

    /** Private constructor: use {@link #getInstance(Path)}. */
    private EUK00361749_HotelSystem(Path dataFile) throws IOException {
        this.dataFile = dataFile;
        this.administrator = new EUK00361749_Admin(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD,
                "Front Office Manager", "+94112345678", "admin@smartstay.lk");
        reloadFromFile();
    }

    /**
     * Returns the one hotel system, creating it on the first call.
     *
     * @param dataFile file used for saving records (only used on the first call)
     */
    public static synchronized EUK00361749_HotelSystem getInstance(Path dataFile) throws IOException {
        if (instance == null) {
            instance = new EUK00361749_HotelSystem(dataFile);
        }
        return instance;
    }

    public EUK00361749_Service getService() {
        return service;
    }

    public Path getDataFile() {
        return dataFile;
    }

    /** @return the administrator if the login is correct, otherwise null. */
    public EUK00361749_Admin loginAdministrator(String username, String password) {
        return administrator.checkCredentials(username, password) ? administrator : null;
    }

    /**
     * Saves every record. If saving fails the in-memory data is reloaded from the last good file,
     * so what the user sees always matches what is on disk.
     */
    public void saveChanges() throws IOException {
        try {
            store.saveToFile(dataFile);
        } catch (IOException ex) {
            reloadFromFile();
            throw new IOException("Save failed, so the change was undone. " + ex.getMessage(), ex);
        }
    }

    private void reloadFromFile() throws IOException {
        store = EUK00361749_Store.loadFromFile(dataFile);
        service = new EUK00361749_Service(store, Clock.system(EUK00361749_Service.HOTEL_ZONE),
                new EUK00361749_LongStayDiscount());
    }
}
