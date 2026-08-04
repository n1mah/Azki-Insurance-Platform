package com.azki.policy.dto;

import com.azki.policy.entity.Policy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        String productName,
        String status,
        BigDecimal premiumAmount,
        String currency,
        LocalDate startDate,
        LocalDate endDate
) {

    public static PolicyResponse from(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getProduct().getName(),
                policy.getStatus().name(),
                policy.getPremiumAmount().getAmount(),
                policy.getPremiumAmount().getCurrency(),
                policy.getStartDate(),
                policy.getEndDate()
        );
    }

}