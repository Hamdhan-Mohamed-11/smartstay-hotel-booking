package smartstay;

/** Bed configurations a room can have. */
public enum EUK00361749_BedType {
    SINGLE("Single"),
    DOUBLE("Double"),
    QUEEN("Queen"),
    KING("King");

    private final String displayName;

    EUK00361749_BedType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
