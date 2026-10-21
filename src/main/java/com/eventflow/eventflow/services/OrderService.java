package com.eventflow.eventflow.services;

import com.eventflow.eventflow.model.Order;
import com.eventflow.eventflow.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getAllOrders(){
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(long orderId){
        return orderRepository.findById(orderId);
    }
}
