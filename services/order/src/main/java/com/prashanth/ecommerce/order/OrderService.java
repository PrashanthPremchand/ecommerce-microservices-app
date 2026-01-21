package com.prashanth.ecommerce.order;

import com.prashanth.ecommerce.customer.CustomerClient;
import com.prashanth.ecommerce.exception.BusinessException;
import com.prashanth.ecommerce.kafka.OrderConfirmation;
import com.prashanth.ecommerce.kafka.OrderProducer;
import com.prashanth.ecommerce.orderline.OrderLineRequest;
import com.prashanth.ecommerce.orderline.OrderLineService;
import com.prashanth.ecommerce.product.ProductClient;
import com.prashanth.ecommerce.product.PurchaseRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderMapper orderMapper;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;

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
        //Todo start payment process
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

    public List<OrderResponse> findAll() {

        return orderRepository.findAll()
                .stream()
                .map(OrderMapper::toOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse findById(Integer orderId) {

        return orderRepository.findById(orderId)
                .map(OrderMapper::toOrderResponse)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

    }
}
