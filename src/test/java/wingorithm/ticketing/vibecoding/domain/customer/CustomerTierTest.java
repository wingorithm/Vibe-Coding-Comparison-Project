package wingorithm.ticketing.vibecoding.domain.customer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerTierTest {

    @Test
    void testBeginnerPricing_NoDiscount() {
        BigDecimal basePrice = new BigDecimal("150000.00");
        BigDecimal finalPrice = CustomerTier.BEGINNER.calculateDiscountedPrice(basePrice);
        assertEquals(new BigDecimal("150000.00"), finalPrice);
    }

    @Test
    void testFansPricing_TenPercentDiscount() {
        BigDecimal basePrice = new BigDecimal("150000.00");
        BigDecimal finalPrice = CustomerTier.FANS.calculateDiscountedPrice(basePrice);
        // 150000 - 15000 = 135000
        assertEquals(new BigDecimal("135000.0000"), finalPrice);
    }

    @Test
    void testLoversPricing_ThirtyPercentDiscount() {
        BigDecimal basePrice = new BigDecimal("150000.00");
        BigDecimal finalPrice = CustomerTier.LOVERS.calculateDiscountedPrice(basePrice);
        // 150000 - 45000 = 105000
        assertEquals(new BigDecimal("105000.0000"), finalPrice);
    }
}
