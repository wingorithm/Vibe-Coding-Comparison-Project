package wingorithm.ticketing.vibecoding.domain.customer;

import java.math.BigDecimal;

public enum CustomerTier {
    BEGINNER(BigDecimal.ZERO),
    FANS(new BigDecimal("0.10")), // 10% discount
    LOVERS(new BigDecimal("0.30")); // 30% discount

    private final BigDecimal discountRate;

    CustomerTier(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    /**
     * Calculates the final price after applying the tier's discount.
     */
    public BigDecimal calculateDiscountedPrice(BigDecimal basePrice) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return basePrice;
        }
        BigDecimal discountAmount = basePrice.multiply(discountRate);
        return basePrice.subtract(discountAmount);
    }
}
