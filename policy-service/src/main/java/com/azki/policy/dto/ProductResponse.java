package com.azki.policy.dto;

import com.azki.policy.entity.InsuranceProduct;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal basePremiumRate,
        String coverageType
) {

    public static ProductResponse from(InsuranceProduct product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBasePremiumRate(),
                product.getCoverageType()
        );
    }

}