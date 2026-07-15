package com.example.demo.entity;

// Replaces dto/PaymentConfirmRequest — lives in entity package
// Passenger sends this after completing UPI payment
public class PaymentConfirmRequest {

    private Long paymentId;   // id returned from /initiate
    private String upiRef;    // UTR / transaction ref from UPI app

    public PaymentConfirmRequest() {}

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getUpiRef() { return upiRef; }
    public void setUpiRef(String upiRef) { this.upiRef = upiRef; }
}