package com.azki.policy.dto;

import jakarta.validation.constraints.NotNull;

public record IssuePolicyRequest(

        @NotNull(message = "productId is required")
        Long productId

) {
}