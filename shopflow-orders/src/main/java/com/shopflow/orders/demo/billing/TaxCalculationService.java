package com.shopflow.orders.demo.billing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single Responsibility: only calculates taxes.
 * Fully testable without database or email dependencies.
 */
@Service
public class TaxCalculationService {

    public record TaxResult(BigDecimal taxAmount, String taxCategory, double taxRate) {}

    public TaxResult calculate(BigDecimal subtotal, String countryCode, String discountCode) {
        double rate = resolveRate(countryCode, discountCode);
        String category = resolveCategory(countryCode, discountCode);
        BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(rate))
                .setScale(2, RoundingMode.HALF_UP);
        return new TaxResult(taxAmount, category, rate);
    }

    private double resolveRate(String countryCode, String discountCode) {
        return switch (countryCode) {
            case "ES" -> resolveSpanishRate(discountCode);
            case "UK" -> 0.20;
            case "DE" -> 0.19;
            default  -> 0.0;
        };
    }

    private double resolveSpanishRate(String discountCode) {
        if (discountCode == null) return 0.21;
        if (discountCode.startsWith("FOOD")) return 0.10;
        if (discountCode.startsWith("MED"))  return 0.04;
        if (discountCode.startsWith("EDU"))  return 0.0;
        return 0.21;
    }

    private String resolveCategory(String countryCode, String discountCode) {
        if (!"ES".equals(countryCode)) return countryCode + "_VAT";
        if (discountCode == null) return "GENERAL";
        if (discountCode.startsWith("FOOD")) return "REDUCIDO";
        if (discountCode.startsWith("MED"))  return "SUPERREDUCIDO";
        if (discountCode.startsWith("EDU"))  return "EXENTO";
        return "GENERAL";
    }
}
