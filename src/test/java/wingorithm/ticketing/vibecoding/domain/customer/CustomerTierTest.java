package wingorithm.ticketing.vibecoding.domain.customer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomerTierTest {

    @Test
    void testBeginnerPricing_Positive() {
        BigDecimal basePrice = new BigDecimal("150000.00");
        BigDecimal finalPrice = CustomerTier.BEGINNER.calculateDiscountedPrice(basePrice);
        assertEquals(new BigDecimal("150000.00"), finalPrice);
    }

    @Test
    void testFansPricing_Positive() {
        BigDecimal basePrice = new BigDecimal("150000.00");
        BigDecimal finalPrice = CustomerTier.FANS.calculateDiscountedPrice(basePrice);
        // 150000 - 15000 = 135000
        assertEquals(new BigDecimal("135000.0000"), finalPrice);
    }

    @Test
    void testLoversPricing_Positive() {
        BigDecimal basePrice = new BigDecimal("150000.00");
        BigDecimal finalPrice = CustomerTier.LOVERS.calculateDiscountedPrice(basePrice);
        // 150000 - 45000 = 105000
        assertEquals(new BigDecimal("105000.0000"), finalPrice);
    }

    @Test
    void testPricing_Negative_NullOrZeroPrice() {
        BigDecimal finalPriceNull = CustomerTier.FANS.calculateDiscountedPrice(null);
        assertNull(finalPriceNull);

        BigDecimal basePriceZero = BigDecimal.ZERO;
        BigDecimal finalPriceZero = CustomerTier.LOVERS.calculateDiscountedPrice(basePriceZero);
        assertEquals(BigDecimal.ZERO, finalPriceZero);

        BigDecimal basePriceNegative = new BigDecimal("-100.00");
        BigDecimal finalPriceNegative = CustomerTier.BEGINNER.calculateDiscountedPrice(basePriceNegative);
        assertEquals(new BigDecimal("-100.00"), finalPriceNegative);
    }
}
