package com.shopflow.orders.demo.billing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Legacy billing service with mixed responsibilities.
 */
@Service
@SuppressWarnings({"all"})
public class LegacyBillingService {

    private final JdbcTemplate jdbcTemplate;

    private int emailsSentToday = 0;
    private static final int MAX_EMAILS_PER_DAY = 1000;

    private static final double IVA_GENERAL = 0.21;
    private static final double IVA_REDUCIDO = 0.10;
    private static final double IVA_SUPERREDUCIDO = 0.04;
    private static final double IVA_EXENTO = 0.0;

    public LegacyBillingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateAndSendInvoice(String orderId, String customerEmail, String countryCode) {
        if (orderId == null || orderId.isEmpty()) {
            throw new RuntimeException("OrderId cannot be empty");
        }
        if (customerEmail == null || !customerEmail.contains("@")) {
            throw new RuntimeException("Invalid email: " + customerEmail);
        }

        String sql = "SELECT * FROM orders WHERE id = '" + orderId + "'";
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching order: " + orderId, e);
        }

        if (rows.isEmpty()) {
            return "ORDER_NOT_FOUND";
        }

        Map<String, Object> order = rows.get(0);
        BigDecimal subtotal = new BigDecimal(order.get("total_amount").toString());
        String status = (String) order.get("status");

        if (!"DELIVERED".equals(status) && !"CONFIRMED".equals(status)) {
            return "INVALID_STATUS_FOR_BILLING:" + status;
        }

        double taxRate;
        String taxCategory;
        if ("ES".equals(countryCode)) {
            String discountCode = (String) order.get("discount_code");
            if (discountCode != null && discountCode.startsWith("FOOD")) {
                taxRate = IVA_REDUCIDO;
                taxCategory = "REDUCIDO";
            } else if (discountCode != null && discountCode.startsWith("MED")) {
                taxRate = IVA_SUPERREDUCIDO;
                taxCategory = "SUPERREDUCIDO";
            } else if (discountCode != null && discountCode.startsWith("EDU")) {
                taxRate = IVA_EXENTO;
                taxCategory = "EXENTO";
            } else {
                taxRate = IVA_GENERAL;
                taxCategory = "GENERAL";
            }
        } else if ("UK".equals(countryCode)) {
            taxRate = 0.20;
            taxCategory = "UK_VAT";
        } else if ("DE".equals(countryCode)) {
            taxRate = 0.19;
            taxCategory = "DE_VAT";
        } else {
            taxRate = 0.0;
            taxCategory = "EXEMPT";
        }

        BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(taxRate))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalWithTax = subtotal.add(taxAmount);

        String invoiceDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String invoiceNumber = "INV-" + invoiceDate + "-" + orderId.substring(0, 8).toUpperCase();

        StringBuilder pdf = new StringBuilder();
        pdf.append("SHOPFLOW INVOICE\n");
        pdf.append("================\n");
        pdf.append("Invoice: ").append(invoiceNumber).append("\n");
        pdf.append("Date: ").append(LocalDate.now()).append("\n");
        pdf.append("Order: ").append(orderId).append("\n");
        pdf.append("Customer: ").append(customerEmail).append("\n");
        pdf.append("Country: ").append(countryCode).append("\n");
        pdf.append("\n");
        pdf.append("Subtotal:   ").append(subtotal).append(" EUR\n");
        pdf.append("Tax (").append(taxCategory).append(" ").append((int)(taxRate * 100)).append("%): ");
        pdf.append(taxAmount).append(" EUR\n");
        pdf.append("TOTAL:      ").append(totalWithTax).append(" EUR\n");
        String pdfContent = pdf.toString();

        try {
            String insertSql = "INSERT INTO invoices (invoice_number, order_id, customer_email, " +
                    "subtotal, tax_amount, total_amount, tax_category, country_code, created_at) VALUES (" +
                    "'" + invoiceNumber + "', " +
                    "'" + orderId + "', " +
                    "'" + customerEmail + "', " +
                    subtotal + ", " +
                    taxAmount + ", " +
                    totalWithTax + ", " +
                    "'" + taxCategory + "', " +
                    "'" + countryCode + "', " +
                    "NOW())";
            jdbcTemplate.execute(insertSql);
        } catch (Exception e) {
            System.err.println("Failed to persist invoice: " + e.getMessage());
        }

        if (emailsSentToday >= MAX_EMAILS_PER_DAY) {
            System.err.println("Daily email limit reached. Invoice not sent to: " + customerEmail);
            return invoiceNumber + ":EMAIL_LIMIT_REACHED";
        }

        try {
            sendEmail(customerEmail, "Your invoice " + invoiceNumber, pdfContent);
            emailsSentToday++;

            jdbcTemplate.execute("UPDATE orders SET status = 'INVOICED' WHERE id = '" + orderId + "'");

        } catch (Exception e) {
            System.err.println("Failed to send invoice email to " + customerEmail + ": " + e.getMessage());
            return invoiceNumber + ":EMAIL_FAILED";
        }

        return invoiceNumber + ":OK";
    }

    public List<String> generateDailyBatch(String countryCode) {
        List<String> results = new ArrayList<>();

        String sql = "SELECT id, customer_id FROM orders WHERE status = 'DELIVERED' " +
                "AND DATE(created_at) = CURRENT_DATE";

        try {
            List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> order : orders) {
                String orderId = order.get("id").toString();
                String email = "customer-" + order.get("customer_id") + "@shopflow.com";
                String result = generateAndSendInvoice(orderId, email, countryCode);
                results.add(result);
            }
        } catch (Exception e) {
            results.add("BATCH_ERROR: " + e.getMessage());
        }

        return results;
    }

    public BigDecimal calculateTax(BigDecimal amount, String countryCode, String category) {
        double rate = switch (countryCode) {
            case "ES" -> switch (category) {
                case "REDUCIDO" -> IVA_REDUCIDO;
                case "SUPERREDUCIDO" -> IVA_SUPERREDUCIDO;
                case "EXENTO" -> IVA_EXENTO;
                default -> IVA_GENERAL;
            };
            case "UK" -> 0.20;
            case "DE" -> 0.19;
            default -> 0.0;
        };
        return amount.multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    public void resetDailyEmailCount() {
        emailsSentToday = 0;
    }

    private void sendEmail(String to, String subject, String body) {
        System.out.println("EMAIL TO: " + to);
        System.out.println("SUBJECT: " + subject);
    }
}
