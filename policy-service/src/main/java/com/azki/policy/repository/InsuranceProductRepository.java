package com.azki.policy.repository;

import com.azki.policy.entity.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
}