package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.infrastructure.payments.PaymentClient;
import com.shopflow.orders.infrastructure.payments.ResilientPaymentClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerPaymentFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResilientPaymentClient resilientPaymentClient;

    @Test
    void should_return_APPROVED_when_payment_is_authorized() throws Exception {
        when(resilientPaymentClient.processPayment(any(), any()))
                .thenAnswer(invocation -> new PaymentClient.PaymentResult(
                        "pay-123",
                        invocation.getArgument(0, UUID.class),
                        true
                ));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId":"11111111-1111-1111-1111-111111111111",
                                  "items":[
                                    {
                                      "productId":"22222222-2222-2222-2222-222222222222",
                                      "quantity":2,
                                      "unitPrice":"10.00"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.paymentId").value("pay-123"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.totalAmount").value("20.00"));

        verify(resilientPaymentClient).processPayment(any(), any());
    }

    @Test
    void should_return_PENDING_when_payment_falls_back() throws Exception {
        when(resilientPaymentClient.processPayment(any(), any()))
                .thenAnswer(invocation -> new PaymentClient.PaymentResult(
                        "PENDING",
                        invocation.getArgument(0, UUID.class),
                        false
                ));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId":"11111111-1111-1111-1111-111111111111",
                                  "items":[
                                    {
                                      "productId":"22222222-2222-2222-2222-222222222222",
                                      "quantity":2,
                                      "unitPrice":"10.00"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentId").value("PENDING"));

        verify(resilientPaymentClient).processPayment(any(), any());
    }
}
