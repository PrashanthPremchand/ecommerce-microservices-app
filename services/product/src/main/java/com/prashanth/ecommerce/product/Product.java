package com.prashanth.ecommerce.product;

import com.prashanth.ecommerce.category.Category;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Product {

    @Id
    @GeneratedValue
    Integer id;
    String name;
    String description;
    double availableQuantity;
    BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "category_id")
    Category category;

}
