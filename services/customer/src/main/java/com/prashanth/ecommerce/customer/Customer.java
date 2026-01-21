package com.prashanth.ecommerce.customer;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@Document
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Customer {

    @Id
    String id;
    String firstName;
    String lastName;
    String email;
    Address address;

}
