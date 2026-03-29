package com.owaspdemo.a10_exceptional_conditions;

import com.owaspdemo.common.model.Order;
import com.owaspdemo.common.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vulnerable/payments")
@Tag(name = "A10 - Exceptional Conditions", description = "Exception swallowing in payment flow vs proper error handling")
public class VulnerablePaymentController {

    private final OrderRepository orderRepository;
    private final PaymentGatewayClient gateway;

    public VulnerablePaymentController(OrderRepository orderRepository, PaymentGatewayClient gateway) {
        this.orderRepository = orderRepository;
        this.gateway = gateway;
    }

    @PostMapping
    @Operation(summary = "Process payment (swallows exceptions)", description = "Amount > $500 triggers gateway timeout but returns 200 + PAID. Silent data corruption.")
    public Map<String, Object> processPayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"userId\": 1, \"productId\": 1, \"amount\": 999.99}")))
            @RequestBody Map<String, Object> body) {
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
    @Operation(summary = "Get order by ID")
    public Map<String, Object> getOrder(
            @Parameter(description = "Order ID", example = "1") @PathVariable Long orderId) {
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
