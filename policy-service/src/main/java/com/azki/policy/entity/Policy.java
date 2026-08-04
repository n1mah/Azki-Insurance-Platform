package com.azki.policy.entity;

import com.azki.policy.valueobject.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "premium_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "premium_currency"))
    })
    private Money premiumAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    protected Policy() {
    }

    public Policy(UUID userId, InsuranceProduct product, Money premiumAmount,
            LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.product = product;
        this.premiumAmount = premiumAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = PolicyStatus.DRAFT;
    }

    public void activate() {
        this.status = PolicyStatus.ACTIVE;
    }

    public void cancel() {
        this.status = PolicyStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public InsuranceProduct getProduct() {
        return product;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public Money getPremiumAmount() {
        return premiumAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

}