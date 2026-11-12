package smartstay;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Abstract Strategy: subclasses supply a rate; the template controls rounding. */
public abstract class EUK00361749_DiscountPolicy {
  public final BigDecimal discount(BigDecimal base, long nights) {
    if (base.signum() < 0 || nights < 1)
      throw new IllegalArgumentException("Invalid discount input.");
    return base.multiply(rateFor(nights)).setScale(2, RoundingMode.HALF_UP);
  }

  protected abstract BigDecimal rateFor(long nights);
}
