package com.owaspdemo.common.repository;

import com.owaspdemo.common.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
