package com.owaspdemo.a10_exceptional_conditions;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Simulated payment gateway that fails intermittently.
 * In a real app, this would call Stripe/PayPal/etc.
 */
@Component
public class PaymentGatewayClient {

    /**
     * Simulates a payment charge. Fails for amounts over $500 to demonstrate
     * error handling differences between vulnerable and secure controllers.
     */
    public String charge(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("500")) > 0) {
            throw new PaymentGatewayTimeoutException(
                    "Gateway timeout: payment processor did not respond within 30s");
        }
        return "ch_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    public static class PaymentGatewayTimeoutException extends RuntimeException {
        public PaymentGatewayTimeoutException(String message) {
            super(message);
        }
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }
}
