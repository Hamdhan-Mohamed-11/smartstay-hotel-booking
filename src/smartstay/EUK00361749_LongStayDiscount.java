package smartstay;

import java.math.BigDecimal;

/** Applies the specified 12 percent discount from the fifth night. */
public final class EUK00361749_LongStayDiscount extends EUK00361749_DiscountPolicy {
  @Override
  protected BigDecimal rateFor(long nights) {
    return nights >= 5 ? new BigDecimal("0.12") : BigDecimal.ZERO;
  }
}
