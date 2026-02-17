package com.prashanth.ecommerce.exception;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CustomerAlreadyExistException extends Throwable {
    public CustomerAlreadyExistException(String msg) {
        super(msg);
    }
}
