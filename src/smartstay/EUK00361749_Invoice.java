package smartstay;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Detailed bill for one booking. All amounts are calculated once in the constructor, following
 * the five steps in the case study, and then only read.
 */
public final class EUK00361749_Invoice {

    /** Refundable advance collected at reservation time (business rule). */
    public static final BigDecimal BOOKING_ADVANCE = new BigDecimal("10000.00");

    private final EUK00361749_Booking booking;
    private final String discountDescription;
    private final int extraGuests;
    private final BigDecimal baseRoomCharge;
    private final BigDecimal extraGuestCharge;
    private final BigDecimal discount;
    private final BigDecimal tax;
    private final BigDecimal totalBeforeAdvance;
    private final BigDecimal finalPayable;
    private final BigDecimal refundDue;

    /**
     * Calculates the bill.
     *
     * @param booking        an active (not cancelled) booking
     * @param discountPolicy the discount rule to apply (Strategy pattern)
     */
    public EUK00361749_Invoice(EUK00361749_Booking booking, EUK00361749_DiscountPolicy discountPolicy) {
        if (booking.isCancelled()) {
            throw new IllegalArgumentException("This booking is cancelled; the advance is refundable and no invoice is issued.");
        }
        this.booking = booking;
        this.discountDescription = discountPolicy.getDescription();

        EUK00361749_RoomType roomType = booking.getRoom().getRoomType();
        BigDecimal nights = BigDecimal.valueOf(booking.getNumberOfNights());

        // Step 1: base room charge = daily rate x number of nights.
        baseRoomCharge = money(roomType.getDailyRate().multiply(nights));

        // Step 2: extra guest charge for guests above the included capacity, per night.
        extraGuests = Math.max(0, booking.getNumberOfGuests() - roomType.getIncludedGuests());
        extraGuestCharge = money(roomType.getExtraGuestCharge()
                .multiply(BigDecimal.valueOf(extraGuests))
                .multiply(nights));

        // Step 3: discount on the base room charge only (before tax).
        discount = discountPolicy.calculateDiscount(baseRoomCharge, booking.getNumberOfNights());

        // Step 4: tax on the discounted subtotal, at the room type's rate.
        BigDecimal taxableAmount = baseRoomCharge.add(extraGuestCharge).subtract(discount);
        tax = money(taxableAmount.multiply(roomType.getTaxRate()));
        totalBeforeAdvance = money(taxableAmount.add(tax));

        // Step 5: deduct the refundable advance; if it exceeds the bill the difference is refunded.
        finalPayable = money(totalBeforeAdvance.subtract(BOOKING_ADVANCE).max(BigDecimal.ZERO));
        refundDue = money(BOOKING_ADVANCE.subtract(totalBeforeAdvance).max(BigDecimal.ZERO));
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public EUK00361749_Booking getBooking() {
        return booking;
    }

    public BigDecimal getBaseRoomCharge() {
        return baseRoomCharge;
    }

    public BigDecimal getExtraGuestCharge() {
        return extraGuestCharge;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getTotalBeforeAdvance() {
        return totalBeforeAdvance;
    }

    public BigDecimal getFinalPayable() {
        return finalPayable;
    }

    public BigDecimal getRefundDue() {
        return refundDue;
    }

    /** Formats the invoice as an aligned text table for the console or a .txt export. */
    @Override
    public String toString() {
        EUK00361749_Room room = booking.getRoom();
        EUK00361749_RoomType roomType = room.getRoomType();
        EUK00361749_Guest guest = booking.getGuest();
        String line = "=".repeat(60);
        String thinLine = "-".repeat(60);
        StringBuilder text = new StringBuilder();
        text.append(line).append('\n')
            .append(String.format("%-35s%25s%n", "SMARTSTAY HOTEL - INVOICE", "Booking " + booking.getBookingId()))
            .append(line).append('\n')
            .append("ROOM\n")
            .append(row("Room ID", room.getRoomId()))
            .append(row("Room type", roomType.toString()))
            .append(row("Bed type", room.getBedType().toString()))
            .append(row("Daily rate", formatMoney(roomType.getDailyRate())))
            .append(row("Complimentary breakfast", roomType.hasComplimentaryBreakfast() ? "Yes" : "No"))
            .append(thinLine).append('\n')
            .append("GUEST\n")
            .append(row("NIC/Passport", guest.getNicOrPassport()))
            .append(row("Name", guest.getFullName()))
            .append(row("Contact", guest.getContactNumber()))
            .append(row("Email", guest.getEmail()))
            .append(thinLine).append('\n')
            .append("STAY\n")
            .append(row("Check-in", booking.getCheckInDate() + " (from 14:00)"))
            .append(row("Check-out", booking.getCheckOutDate().toString()))
            .append(row("Number of nights", String.valueOf(booking.getNumberOfNights())))
            .append(row("Number of guests", booking.getNumberOfGuests()
                    + " (" + roomType.getIncludedGuests() + " included)"))
            .append(thinLine).append('\n')
            .append("CHARGES\n")
            .append(row("Base room charge", formatMoney(baseRoomCharge)))
            .append(row("Extra guest charge (" + extraGuests + " extra)", formatMoney(extraGuestCharge)))
            .append(row("Discount applied", "- " + formatMoney(discount)))
            .append(row("  " + (discount.signum() > 0 ? discountDescription : "not eligible (under 5 nights)"), ""))
            .append(row("Tax added (" + roomType.getTaxRate().movePointRight(2).stripTrailingZeros().toPlainString() + "%)",
                    formatMoney(tax)))
            .append(row("Total", formatMoney(totalBeforeAdvance)))
            .append(row("Booking advance deducted", "- " + formatMoney(BOOKING_ADVANCE)))
            .append(line).append('\n')
            .append(row("FINAL PAYABLE AMOUNT", formatMoney(finalPayable)));
        if (refundDue.signum() > 0) {
            text.append(row("Refund due to guest", formatMoney(refundDue)));
        }
        text.append(line);
        return text.toString();
    }

    private static String row(String label, String value) {
        return String.format("  %-34s%24s%n", label, value);
    }

    /** Formats an amount as "LKR 12,345.00". */
    public static String formatMoney(BigDecimal amount) {
        return String.format("LKR %,.2f", amount);
    }
}
