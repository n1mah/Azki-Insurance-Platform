package com.azki.policy.exception;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(java.util.UUID policyId) {
        super("policy not found: " + policyId);
    }

}