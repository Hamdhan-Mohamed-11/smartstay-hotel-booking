package smartstay;

import java.math.BigDecimal;

/** Business rule: stays of 5 or more nights get 12% off the base room charge (before tax). */
public final class EUK00361749_LongStayDiscount extends EUK00361749_DiscountPolicy {

    public static final int MINIMUM_NIGHTS = 5;
    private static final BigDecimal LONG_STAY_RATE = new BigDecimal("0.12");

    @Override
    protected BigDecimal getDiscountRate(long numberOfNights) {
        return numberOfNights >= MINIMUM_NIGHTS ? LONG_STAY_RATE : BigDecimal.ZERO;
    }

    @Override
    public String getDescription() {
        return "12% long-stay discount (5+ nights)";
    }
}
