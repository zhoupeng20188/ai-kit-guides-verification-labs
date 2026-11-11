package demo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PriceTest {

    @Test
    void aboveBoundaryQualifies() {
        assertTrue(Price.qualifies(10001));
    }
}
