package com.prashanth.ecommerce.auth;

import com.prashanth.ecommerce.customer.Customer;
import com.prashanth.ecommerce.customer.CustomerRepository;
import com.prashanth.ecommerce.customer.CustomerService;
import com.prashanth.ecommerce.exception.CustomerAlreadyExistException;
import com.prashanth.ecommerce.role.Role;
import com.prashanth.ecommerce.security.JWTUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;


    public void register(RegisterRequest request) throws CustomerAlreadyExistException {

        if(customerService.existsByEmail(request.getEmail())) {

            throw new CustomerAlreadyExistException("Customer already exists with email: " + request.getEmail());

        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .address(request.getAddress())
                .build();

        customerRepository.save(customer);



    }

    public AuthResponse login(@Valid LoginRequest request) {

        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Customer customer = (Customer) auth.getPrincipal();

        String token = jwtUtil.generateToken(customer);

        return new AuthResponse(token);

    }
}
