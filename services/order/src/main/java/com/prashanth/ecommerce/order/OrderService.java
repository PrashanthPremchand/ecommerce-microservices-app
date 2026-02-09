package com.prashanth.ecommerce.order;

import com.prashanth.ecommerce.customer.CustomerClient;
import com.prashanth.ecommerce.exception.BusinessException;
import com.prashanth.ecommerce.kafka.OrderConfirmation;
import com.prashanth.ecommerce.kafka.OrderProducer;
import com.prashanth.ecommerce.orderline.OrderLineRequest;
import com.prashanth.ecommerce.orderline.OrderLineService;
import com.prashanth.ecommerce.payment.PaymentClient;
import com.prashanth.ecommerce.payment.PaymentRequest;
import com.prashanth.ecommerce.product.ProductClient;
import com.prashanth.ecommerce.product.PurchaseRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderMapper orderMapper;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;
    private final PaymentClient paymentClient;

    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    public Integer createOrder(OrderRequest request) {

        var customer = customerClient.findCustomerById(request.customerId())
                .orElseThrow(() -> new BusinessException("Cannot create order with id " + request.customerId()));
        var purchasedProducts = productClient.purchaseProducts(request.products());
        var order = orderRepository.save(orderMapper.toOrder(request));
        for(PurchaseRequest purchaseRequest: request.products()){
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            null,
                            order.getId(),
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }
        var paymentRequest = new PaymentRequest(
                request.amount(),
                request.paymentMethod(),
                order.getId(),
                order.getReference(),
                customer
        );
        paymentClient.requestOrderPayment(paymentRequest);
        orderProducer.sendOrderConformation(
                new OrderConfirmation(
                        request.reference(),
                        request.amount(),
                        request.paymentMethod(),
                        customer,
                        purchasedProducts
                )
        );
        return order.getId();

    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "findAllFallback")
    public List<OrderResponse> findAll() {

        return orderRepository.findAll()
                .stream()
                .map(OrderMapper::toOrderResponse)
                .collect(Collectors.toList());
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "findByIdFallback")
    public OrderResponse findById(Integer orderId) {

        return orderRepository.findById(orderId)
                .map(OrderMapper::toOrderResponse)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

    }

    private Integer createOrderFallback(OrderRequest request, Exception ex) {
        log.error("Circuit breaker activated for createOrder. Request: {}, Error: {}",
                request.reference(), ex.getMessage());
        throw new BusinessException("Service temporarily unavailable. Please try again later.");
    }

    private List<OrderResponse> findAllFallback(Exception ex) {
        log.error("Circuit breaker activated for findAll. Error: {}", ex.getMessage());
        return List.of(); // Return empty list as fallback
    }

    private OrderResponse findByIdFallback(Integer orderId, Exception ex) {
        log.error("Circuit breaker activated for findById. OrderId: {}, Error: {}",
                orderId, ex.getMessage());
        throw new BusinessException("Unable to retrieve order. Please try again later.");
    }

}
