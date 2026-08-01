package com.azki.policy.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("insurance product not found: " + productId);
    }

}