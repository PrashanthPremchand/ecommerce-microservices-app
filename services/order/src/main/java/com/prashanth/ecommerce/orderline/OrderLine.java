package com.prashanth.ecommerce.orderline;

import com.prashanth.ecommerce.order.Order;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class OrderLine {

    @Id
    @GeneratedValue
    Integer id;
    Double quantity;

    @ManyToOne
    @JoinColumn(name = "order_id")
    Order order;
    Integer productId;


}
