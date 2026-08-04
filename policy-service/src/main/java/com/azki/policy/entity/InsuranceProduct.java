package com.azki.policy.entity;

import com.azki.policy.valueobject.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "insurance_products")
public class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "base_premium_rate")),
            @AttributeOverride(name = "currency", column = @Column(name = "base_premium_currency"))
    })
    private Money basePremiumRate;

    @Column(name = "coverage_type", nullable = false)
    private String coverageType;

    protected InsuranceProduct() {
    }

    public InsuranceProduct(String name, Money basePremiumRate, String coverageType) {
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

    public Money getBasePremiumRate() {
        return basePremiumRate;
    }

    public String getCoverageType() {
        return coverageType;
    }

}