package com.azki.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "insurance_products")
public class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_premium_rate", nullable = false)
    private BigDecimal basePremiumRate;

    @Column(name = "coverage_type", nullable = false)
    private String coverageType;

    protected InsuranceProduct() {
    }

    public InsuranceProduct(String name, BigDecimal basePremiumRate, String coverageType) {
        this.name = name;
        this.basePremiumRate = basePremiumRate;
        this.coverageType = coverageType;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBasePremiumRate() {
        return basePremiumRate;
    }

    public String getCoverageType() {
        return coverageType;
    }

}