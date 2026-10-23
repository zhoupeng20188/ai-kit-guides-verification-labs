package guides;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PricingTest {
    @Test void belowThreshold() { assertEquals(new BigDecimal("0.00"), Pricing.discount(new BigDecimal("99.99"))); }
    @Test void exactBoundary() { assertEquals(new BigDecimal("10.00"), Pricing.discount(new BigDecimal("100.00"))); }
    @Test void aboveThresholdRounds() { assertEquals(new BigDecimal("10.01"), Pricing.discount(new BigDecimal("100.05"))); }
    @Test void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> Pricing.discount(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> Pricing.discount(null));
    }
}
