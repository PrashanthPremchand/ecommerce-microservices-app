package com.prashanth.ecommerce.kafka;

import com.prashanth.ecommerce.customer.CustomerResponse;
import com.prashanth.ecommerce.order.PaymentMethod;
import com.prashanth.ecommerce.product.PurchaseResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        CustomerResponse customer,
        List<PurchaseResponse> products
) {
}
