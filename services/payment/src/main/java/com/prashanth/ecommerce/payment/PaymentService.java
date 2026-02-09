package com.prashanth.ecommerce.payment;

import com.prashanth.ecommerce.notification.NotificationProducer;
import com.prashanth.ecommerce.notification.PaymentNotificationRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final NotificationProducer notificationProducer;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "createPaymentFallback")
    public Integer createPayment(@Valid PaymentRequest paymentRequest) {

        var payment = paymentRepository.save(paymentMapper.toPayment(paymentRequest));
        notificationProducer.sendNotification(
                new PaymentNotificationRequest(
                        paymentRequest.orderReference(),
                        paymentRequest.amount(),
                        paymentRequest.paymentMethod(),
                        paymentRequest.customer().firstName(),
                        paymentRequest.customer().lastName(),
                        paymentRequest.customer().email()
                )
        );
        return payment.getId();

    }

    public Integer createPaymentFallback(PaymentRequest paymentRequest, Exception e) {
        log.error("Circuit breaker activated. Saving payment without notification for order: {}",
                paymentRequest.orderReference(), e);
        return paymentRepository.save(paymentMapper.toPayment(paymentRequest)).getId();
    }

}
