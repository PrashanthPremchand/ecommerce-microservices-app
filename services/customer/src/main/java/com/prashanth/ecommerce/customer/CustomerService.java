package com.prashanth.ecommerce.customer;


import com.prashanth.ecommerce.exception.CustomerNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper mapper;

    @CircuitBreaker(name = "customerService", fallbackMethod = "createCustomerFallback")
    public String createCustomer(@Valid CustomerRequest request) {
        var customer = customerRepository.save(mapper.toCustomer(request));
        return customer.getId();
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "updateCustomerFallback")
    public void updateCustomer(@Valid CustomerRequest request) {
        var customer = customerRepository.findById(request.id())
                .orElseThrow(() -> new CustomerNotFoundException(
                        format("Customer with id %s not found", request.id())
                ));
        mergeCustomer(customer, request);
        customerRepository.save(customer);
    }

    private void mergeCustomer(Customer customer, @Valid CustomerRequest request) {
        if(StringUtils.isNotBlank(request.firstName())){
            customer.setFirstName(request.firstName());
        }
        if(StringUtils.isNotBlank(request.lastName())){
            customer.setLastName(request.lastName());
        }
        if(request.address() != null){
            customer.setAddress(request.address());
        }
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "findAllCustomersFallback")
    public List<CustomerResponse> findAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(mapper::toCustomerResponse)
                .collect(Collectors.toList());
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "existsByIdFallback")
    public Boolean existsById(String customerId) {
        return customerRepository.existsById(customerId);
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "findByIdFallback")
    public CustomerResponse findById(String customerId) {
        return customerRepository.findById(customerId).map(mapper::toCustomerResponse)
                .orElseThrow(() -> new CustomerNotFoundException(format("Customer with id %s not found", customerId)));
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "deleteByIdFallback")
    public void deleteById(String customerId) {
        customerRepository.deleteById(customerId);
    }

    private String createCustomerFallback(CustomerRequest request, Exception ex) {
        log.error("Circuit breaker fallback - Unable to create customer: {}", ex.getMessage());
        throw new RuntimeException("Customer service is currently unavailable. Please try again later.");
    }

    private void updateCustomerFallback(CustomerRequest request, Exception ex) {
        log.error("Circuit breaker fallback - Unable to update customer: {}", ex.getMessage());
        throw new RuntimeException("Customer service is currently unavailable. Please try again later.");
    }

    private List<CustomerResponse> findAllCustomersFallback(Exception ex) {
        log.error("Circuit breaker fallback - Unable to fetch customers: {}", ex.getMessage());
        return Collections.emptyList();
    }

    private Boolean existsByIdFallback(String customerId, Exception ex) {
        log.error("Circuit breaker fallback - Unable to check customer existence: {}", ex.getMessage());
        return false;
    }

    private CustomerResponse findByIdFallback(String customerId, Exception ex) {
        log.error("Circuit breaker fallback - Unable to find customer: {}", ex.getMessage());
        throw new RuntimeException("Customer service is currently unavailable. Please try again later.");
    }

    private void deleteByIdFallback(String customerId, Exception ex) {
        log.error("Circuit breaker fallback - Unable to delete customer: {}", ex.getMessage());
        throw new RuntimeException("Customer service is currently unavailable. Please try again later.");
    }
}
