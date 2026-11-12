package smartstay;

import java.math.BigDecimal;

/**
 * Room packages from Table 1 of the case study. Each constant carries its own tariff, so pricing
 * data lives in exactly one place.
 *
 * <p>Included-guest capacity is not given in the brief; the values below are a documented
 * assumption (Standard 2, Deluxe 2, Suite 3, Family Suite 4).
 */
public enum EUK00361749_RoomType {

    //            display name   daily rate  extra guest  tax     included  breakfast
    STANDARD("Standard", 6000, 1000, "0.10", 2, false),
    DELUXE("Deluxe", 9000, 1500, "0.12", 2, true),
    SUITE("Suite", 14000, 2000, "0.15", 3, true),
    FAMILY_SUITE("Family Suite", 18000, 2500, "0.15", 4, true);

    /** Maximum guests allowed above the included capacity (assumption). */
    public static final int MAX_EXTRA_GUESTS = 2;

    private final String displayName;
    private final BigDecimal dailyRate;
    private final BigDecimal extraGuestCharge;
    private final BigDecimal taxRate;
    private final int includedGuests;
    private final boolean complimentaryBreakfast;

    EUK00361749_RoomType(String displayName, int dailyRate, int extraGuestCharge, String taxRate,
                         int includedGuests, boolean complimentaryBreakfast) {
        this.displayName = displayName;
        this.dailyRate = BigDecimal.valueOf(dailyRate);
        this.extraGuestCharge = BigDecimal.valueOf(extraGuestCharge);
        this.taxRate = new BigDecimal(taxRate);
        this.includedGuests = includedGuests;
        this.complimentaryBreakfast = complimentaryBreakfast;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    /** Charge per extra guest per night. */
    public BigDecimal getExtraGuestCharge() {
        return extraGuestCharge;
    }

    /** Tax as a fraction, e.g. 0.10 for 10%. */
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public int getIncludedGuests() {
        return includedGuests;
    }

    /** Highest guest count accepted for this room type. */
    public int getMaximumGuests() {
        return includedGuests + MAX_EXTRA_GUESTS;
    }

    public boolean hasComplimentaryBreakfast() {
        return complimentaryBreakfast;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
