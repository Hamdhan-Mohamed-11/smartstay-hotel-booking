package smartstay;

import java.math.BigDecimal;

/** Tariffs from the coursework; capacities are documented assumptions. */
public enum EUK00361749_RoomType {
  STANDARD(6000, 1000, "0.10", 2, false),
  DELUXE(9000, 1500, "0.12", 2, true),
  SUITE(14000, 2000, "0.15", 3, true),
  FAMILY_SUITE(18000, 2500, "0.15", 4, true);
  private final int rate, extra, capacity;
  private final BigDecimal tax;
  private final boolean breakfast;

  EUK00361749_RoomType(int rate, int extra, String tax, int capacity, boolean breakfast) {
    this.rate = rate;
    this.extra = extra;
    this.tax = new BigDecimal(tax);
    this.capacity = capacity;
    this.breakfast = breakfast;
  }

  public BigDecimal dailyRate() {
    return BigDecimal.valueOf(rate);
  }

  public BigDecimal extraRate() {
    return BigDecimal.valueOf(extra);
  }

  public BigDecimal taxRate() {
    return tax;
  }

  public int capacity() {
    return capacity;
  }

  public boolean breakfast() {
    return breakfast;
  }
}
