package com.prashanth.ecommerce.notification;

import com.prashanth.ecommerce.kafka.order.OrderConfirmation;
import com.prashanth.ecommerce.kafka.payment.PaymentConfirmation;
import org.springframework.data.annotation.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@Document
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {

    @Id
    String id;
    NotificationType type;
    LocalDateTime notificationDate;
    OrderConfirmation orderConfirmation;
    PaymentConfirmation paymentConfirmation;

}
