package com.owaspdemo.a10_exceptional_conditions;

import com.owaspdemo.common.model.Order;
import com.owaspdemo.common.repository.OrderRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * A10:2025 - Mishandling of Exceptional Conditions
 *
 * VULNERABLE: Swallows exceptions, returns 200, and leaves data in an inconsistent state.
 *
 * Try: POST /api/v1/vulnerable/payments with amount > 500 (triggers gateway timeout)
 * Result: HTTP 200 returned, order status = "PAID", but no actual charge was made!
 *
 * This is a real-world pattern that causes revenue loss and customer disputes.
 */
@RestController
@RequestMapping("/api/v1/vulnerable/payments")
public class VulnerablePaymentController {

    private final OrderRepository orderRepository;
    private final PaymentGatewayClient gateway;

    public VulnerablePaymentController(OrderRepository orderRepository, PaymentGatewayClient gateway) {
        this.orderRepository = orderRepository;
        this.gateway = gateway;
    }

    @PostMapping
    public Map<String, Object> processPayment(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long productId = Long.valueOf(body.get("productId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        Order order = new Order(userId, productId, amount, "PENDING", UUID.randomUUID().toString());
        orderRepository.save(order);

        // BAD: Empty catch block swallows the exception
        try {
            gateway.charge(amount);
        } catch (Exception e) {
            // BAD: Exception swallowed — charge failed but we continue as if it succeeded
        }

        // BAD: Order marked as PAID regardless of whether the charge succeeded
        order.setStatus("PAID");
        orderRepository.save(order);

        // BAD: Returns 200 OK even though payment may have failed
        return Map.of(
                "status", "PAID",
                "orderId", order.getId(),
                "message", "Payment processed successfully"
        );
    }

    @GetMapping("/{orderId}")
    public Map<String, Object> getOrder(@PathVariable Long orderId) {
        return orderRepository.findById(orderId)
                .map(o -> Map.<String, Object>of(
                        "orderId", o.getId(),
                        "status", o.getStatus(),
                        "amount", o.getAmount(),
                        "correlationId", o.getCorrelationId()
                ))
                .orElse(Map.of("error", "Order not found"));
    }
}
