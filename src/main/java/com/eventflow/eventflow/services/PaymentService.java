package com.eventflow.eventflow.services;

import com.eventflow.eventflow.model.Payment;
import com.eventflow.eventflow.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(long paymentId) {
        return paymentRepository.findById(paymentId);
    }
}
