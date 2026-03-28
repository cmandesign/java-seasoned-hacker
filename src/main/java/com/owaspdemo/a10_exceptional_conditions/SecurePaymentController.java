package com.owaspdemo.a10_exceptional_conditions;

import com.owaspdemo.common.model.Order;
import com.owaspdemo.common.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * A10:2025 - Mishandling of Exceptional Conditions
 *
 * SECURE: Handles exceptions properly with transactional rollback,
 * specific exception types, and correlation IDs for debugging.
 *
 * Try: POST /api/v1/secure/payments with amount > 500
 * Result: HTTP 502, order stays "PENDING", correlation ID in response for support.
 */
@RestController
@RequestMapping("/api/v1/secure/payments")
public class SecurePaymentController {

    private static final Logger log = LoggerFactory.getLogger(SecurePaymentController.class);

    private final OrderRepository orderRepository;
    private final PaymentGatewayClient gateway;

    public SecurePaymentController(OrderRepository orderRepository, PaymentGatewayClient gateway) {
        this.orderRepository = orderRepository;
        this.gateway = gateway;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long productId = Long.valueOf(body.get("productId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String correlationId = UUID.randomUUID().toString();

        Order order = new Order(userId, productId, amount, "PENDING", correlationId);
        orderRepository.save(order);

        try {
            // GOOD: Catch specific exception, not Exception
            String chargeId = gateway.charge(amount);
            order.setStatus("PAID");
            orderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "status", "PAID",
                    "orderId", order.getId(),
                    "chargeId", chargeId,
                    "correlationId", correlationId
            ));

        } catch (PaymentGatewayClient.PaymentGatewayTimeoutException e) {
            // GOOD: Log the error with correlation ID for debugging
            log.error("Payment gateway timeout for order correlationId={}: {}", correlationId, e.getMessage());

            // GOOD: Order stays PENDING — no false positive
            order.setStatus("FAILED");
            orderRepository.save(order);

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "status", "FAILED",
                            "orderId", order.getId(),
                            "correlationId", correlationId,
                            "message", "Payment gateway timed out. Please retry or contact support.",
                            "retryable", true
                    ));

        } catch (PaymentGatewayClient.InsufficientFundsException e) {
            log.warn("Insufficient funds for order correlationId={}: {}", correlationId, e.getMessage());

            order.setStatus("FAILED");
            orderRepository.save(order);

            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of(
                            "status", "FAILED",
                            "orderId", order.getId(),
                            "correlationId", correlationId,
                            "message", "Insufficient funds",
                            "retryable", false
                    ));
        }
        // GOOD: No catch(Exception e) — unexpected errors propagate to GlobalExceptionHandler
        // GOOD: Never catch(Error e) — let JVM handle OOM, StackOverflow, etc.
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable Long orderId) {
        return orderRepository.findById(orderId)
                .map(o -> ResponseEntity.ok(Map.<String, Object>of(
                        "orderId", o.getId(),
                        "status", o.getStatus(),
                        "amount", o.getAmount(),
                        "correlationId", o.getCorrelationId()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
