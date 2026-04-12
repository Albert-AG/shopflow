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
 * MATERIAL DIDÁCTICO — Tema 15: Refactorización y Deuda Técnica
 *
 * Este archivo es INTENCIONALMENTE MALO — Ejercicio del Tema 15.
 *
 * God Object: mezcla lógica fiscal, generación PDF, persistencia JDBC,
 * envío de email y validación en una sola clase.
 *
 * Problemas identificados para el ejercicio de refactorización:
 * 1. Una sola clase con ~7 responsabilidades distintas (SRP violado)
 * 2. Java 8 style: for loops, checked exceptions silenciadas, raw types
 * 3. Lógica fiscal hardcodeada (tipos de IVA como magic numbers)
 * 4. JDBC puro sin Spring Data, SQL concatenado (SQL injection)
 * 5. Sin tests (objetivo T15: generar tests de caracterización antes de tocar)
 * 6. Estado mutable compartido (emailsSentToday)
 * 7. PDF "generado" con String concatenación
 *
 * Flujo de refactorización T15 (Agent Mode):
 * Paso 1: generar tests de caracterización (NO modificar lógica)
 * Paso 2: extraer TaxCalculationService
 * Paso 3: extraer InvoicePdfGenerator
 * Paso 4: extraer InvoiceEmailSender
 * Paso 5: modernizar con streams y Records
 *
 * NO modificar sin haber generado los tests primero.
 */
@Service
@SuppressWarnings({"all"})
public class LegacyBillingService {

    private final JdbcTemplate jdbcTemplate;

    // Estado mutable compartido — problema de concurrencia
    private int emailsSentToday = 0;
    private static final int MAX_EMAILS_PER_DAY = 1000;

    // Magic numbers — deberían ser configuración o enum
    private static final double IVA_GENERAL = 0.21;
    private static final double IVA_REDUCIDO = 0.10;
    private static final double IVA_SUPERREDUCIDO = 0.04;
    private static final double IVA_EXENTO = 0.0;

    public LegacyBillingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Genera y envía una factura para un pedido.
     * Este método hace TODO: calcula impuestos, genera PDF, persiste, envía email.
     */
    public String generateAndSendInvoice(String orderId, String customerEmail, String countryCode) {
        // Validación manual
        if (orderId == null || orderId.isEmpty()) {
            throw new RuntimeException("OrderId cannot be empty");
        }
        if (customerEmail == null || !customerEmail.contains("@")) {
            throw new RuntimeException("Invalid email: " + customerEmail);
        }

        // Fetch order data — SQL concatenado (SQL Injection)
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

        // Validación de estado del pedido
        if (!"DELIVERED".equals(status) && !"CONFIRMED".equals(status)) {
            return "INVALID_STATUS_FOR_BILLING:" + status;
        }

        // ---- Cálculo de impuestos (debería ser TaxCalculationService) ----
        double taxRate;
        String taxCategory;
        if ("ES".equals(countryCode)) {
            // Categorización de productos simplificada
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

        // ---- Generación del número de factura ----
        String invoiceDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String invoiceNumber = "INV-" + invoiceDate + "-" + orderId.substring(0, 8).toUpperCase();

        // ---- "Generación de PDF" (String concatenación — debería ser InvoicePdfGenerator) ----
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

        // ---- Persistencia — JDBC puro, SQL concatenado ----
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
            // Error silenciado — la factura se genera aunque no se persista
            System.err.println("Failed to persist invoice: " + e.getMessage());
        }

        // ---- Envío de email (debería ser InvoiceEmailSender) ----
        if (emailsSentToday >= MAX_EMAILS_PER_DAY) {
            System.err.println("Daily email limit reached. Invoice not sent to: " + customerEmail);
            return invoiceNumber + ":EMAIL_LIMIT_REACHED";
        }

        try {
            // Simulación de envío de email
            sendEmail(customerEmail, "Your invoice " + invoiceNumber, pdfContent);
            emailsSentToday++; // no thread-safe

            // Marcar pedido como facturado — SQL concatenado
            jdbcTemplate.execute("UPDATE orders SET status = 'INVOICED' WHERE id = '" + orderId + "'");

        } catch (Exception e) {
            System.err.println("Failed to send invoice email to " + customerEmail + ": " + e.getMessage());
            return invoiceNumber + ":EMAIL_FAILED";
        }

        return invoiceNumber + ":OK";
    }

    /**
     * Genera facturas en batch para todos los pedidos DELIVERED del día.
     * Método adicional que comparte la lógica de la clase sin refactorizar.
     */
    public List<String> generateDailyBatch(String countryCode) {
        List<String> results = new ArrayList<>();

        // Sin paginación — trae todos los registros
        String sql = "SELECT id, customer_id FROM orders WHERE status = 'DELIVERED' " +
                "AND DATE(created_at) = CURRENT_DATE";

        try {
            List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> order : orders) {
                String orderId = order.get("id").toString();
                // Email hardcodeado — en el código real vendría del servicio de customers
                String email = "customer-" + order.get("customer_id") + "@shopflow.com";
                String result = generateAndSendInvoice(orderId, email, countryCode);
                results.add(result);
            }
        } catch (Exception e) {
            results.add("BATCH_ERROR: " + e.getMessage());
        }

        return results;
    }

    /**
     * Calcula el IVA aplicable. Duplicado de la lógica inline de arriba.
     * La IA generó esto como método separado pero la lógica sigue duplicada.
     */
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

    /**
     * Reinicia el contador de emails diarios.
     * Debería llamarse con un @Scheduled, pero aquí está como método público.
     */
    public void resetDailyEmailCount() {
        emailsSentToday = 0;
    }

    private void sendEmail(String to, String subject, String body) {
        // Simulación: en producción llamaría a SMTP o SendGrid
        System.out.println("EMAIL TO: " + to);
        System.out.println("SUBJECT: " + subject);
        // Body truncado para no inundar los logs
    }
}
