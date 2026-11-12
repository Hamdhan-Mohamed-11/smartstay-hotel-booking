package smartstay;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Abstract discount rule (Strategy design pattern).
 *
 * <p>The invoice does not know which discount rule is in force; it simply asks the injected policy
 * for a discount. A new promotion can therefore be added as another subclass without changing the
 * invoice or service code. The final {@link #calculateDiscount} method fixes the validation and
 * rounding, while subclasses only decide the rate.
 */
public abstract class EUK00361749_DiscountPolicy {

    /**
     * Calculates the discount on the base room charge.
     *
     * @param baseRoomCharge daily rate x nights (before tax)
     * @param numberOfNights length of stay
     * @return discount amount rounded to 2 decimal places
     */
    public final BigDecimal calculateDiscount(BigDecimal baseRoomCharge, long numberOfNights) {
        if (baseRoomCharge.signum() < 0 || numberOfNights < 1) {
            throw new IllegalArgumentException("Invalid discount input.");
        }
        return baseRoomCharge.multiply(getDiscountRate(numberOfNights)).setScale(2, RoundingMode.HALF_UP);
    }

    /** @return the discount rate as a fraction (e.g. 0.12) for the given length of stay. */
    protected abstract BigDecimal getDiscountRate(long numberOfNights);

    /** @return a short human-readable description shown on invoices. */
    public abstract String getDescription();
}
