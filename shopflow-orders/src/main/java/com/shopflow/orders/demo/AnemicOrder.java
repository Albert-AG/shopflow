package com.shopflow.orders.demo;

import java.util.ArrayList;
import java.util.List;

public class AnemicOrder {

    private String id;
    private String customerId;
    private String status;
    private Double totalAmount;
    private List<Object> items = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public List<Object> getItems() { return items; }
    public void setItems(List<Object> items) { this.items = items; }
}
