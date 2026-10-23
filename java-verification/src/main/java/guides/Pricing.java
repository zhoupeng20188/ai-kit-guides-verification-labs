package guides;
import java.math.BigDecimal;
import java.math.RoundingMode;
public final class Pricing {
    private Pricing() {}
    public static BigDecimal discount(BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw new IllegalArgumentException("Non-negative subtotal required");
        }
        return subtotal.compareTo(new BigDecimal("100.00")) >= 0
                ? subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
                : new BigDecimal("0.00");
    }
}
