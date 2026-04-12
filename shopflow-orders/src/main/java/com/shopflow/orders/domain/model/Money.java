package com.shopflow.orders.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Value Object representing a monetary amount with currency.
 * Immutable. Arithmetic operations return new instances.
 */
public record Money(BigDecimal amount, Currency currency) {

    private static final Currency EUR = Currency.getInstance("EUR");

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
        // Normalize to 4 decimal places (as per schema DECIMAL(19,4))
        amount = amount.setScale(4, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, EUR);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, EUR);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Factor cannot be negative: " + factor);
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public Money applyDiscount(int discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Discount percent must be between 0 and 100");
        }
        BigDecimal factor = BigDecimal.ONE.subtract(
            BigDecimal.valueOf(discountPercent).divide(BigDecimal.valueOf(100))
        );
        return new Money(this.amount.multiply(factor).setScale(4, RoundingMode.HALF_UP), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot operate on different currencies: " + this.currency + " vs " + other.currency
            );
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
