package smartstay;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Owned calculation snapshot created on demand, never a payment gateway. */
public final class EUK00361749_Invoice {
  public static final BigDecimal ADVANCE = new BigDecimal("10000.00");
  private final EUK00361749_Booking booking;
  private final BigDecimal base, extra, discount, tax, total, payable, refund;

  public EUK00361749_Invoice(EUK00361749_Booking booking, EUK00361749_DiscountPolicy policy) {
    if (booking.isCancelled())
      throw new IllegalArgumentException("Cancelled booking: advance refunded; no active invoice.");
    this.booking = booking;
    EUK00361749_RoomType t = booking.getRoom().getType();
    base = t.dailyRate().multiply(BigDecimal.valueOf(booking.nights()));
    extra =
        t.extraRate()
            .multiply(BigDecimal.valueOf(Math.max(0, booking.getGuests() - t.capacity())))
            .multiply(BigDecimal.valueOf(booking.nights()));
    discount = policy.discount(base, booking.nights());
    BigDecimal taxable = base.add(extra).subtract(discount);
    tax = taxable.multiply(t.taxRate()).setScale(2, RoundingMode.HALF_UP);
    total = taxable.add(tax).setScale(2, RoundingMode.HALF_UP);
    payable = total.subtract(ADVANCE).max(BigDecimal.ZERO).setScale(2);
    refund = ADVANCE.subtract(total).max(BigDecimal.ZERO).setScale(2);
  }

  public BigDecimal getBase() {
    return base;
  }

  public BigDecimal getExtra() {
    return extra;
  }

  public BigDecimal getDiscount() {
    return discount;
  }

  public BigDecimal getTax() {
    return tax;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public BigDecimal getPayable() {
    return payable;
  }

  public BigDecimal getRefund() {
    return refund;
  }

  @Override
  public String toString() {
    return "SMARTSTAY INVOICE | "
        + booking.getId()
        + "\nRoom: "
        + booking.getRoom()
        + "\nGuest: "
        + booking.getGuest()
        + "\nCheck-in: "
        + booking.getCheckIn()
        + " 14:00 Asia/Colombo"
        + "\nCheck-out: "
        + booking.getCheckOut()
        + " | Nights: "
        + booking.nights()
        + " | Guests: "
        + booking.getGuests()
        + "\nBase: LKR "
        + base
        + "\nExtra guests: LKR "
        + extra
        + "\nDiscount: LKR "
        + discount
        + "\nTax ("
        + booking.getRoom().getType().taxRate().multiply(new BigDecimal("100"))
        + "%): LKR "
        + tax
        + "\nTotal: LKR "
        + total
        + "\nAdvance deducted: LKR "
        + ADVANCE
        + "\nFinal payable: LKR "
        + payable
        + "\nRefund due: LKR "
        + refund;
  }
}
