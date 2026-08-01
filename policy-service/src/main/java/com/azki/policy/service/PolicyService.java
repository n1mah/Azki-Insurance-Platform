package com.azki.policy.service;

import com.azki.policy.entity.InsuranceProduct;
import com.azki.policy.entity.Policy;
import com.azki.policy.exception.PolicyNotFoundException;
import com.azki.policy.exception.ProductNotFoundException;
import com.azki.policy.repository.InsuranceProductRepository;
import com.azki.policy.repository.PolicyRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final InsuranceProductRepository productRepository;

    public PolicyService(PolicyRepository policyRepository, InsuranceProductRepository productRepository) {
        this.policyRepository = policyRepository;
        this.productRepository = productRepository;
    }

    @Cacheable("insuranceProducts")
    public List<InsuranceProduct> getAvailableProducts() {
        return productRepository.findAll();
    }

    public Policy issuePolicy(UUID userId, Long productId) {
        InsuranceProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Policy policy = new Policy(
                userId,
                product,
                product.getBasePremiumRate(),
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );
        policy.activate();

        return policyRepository.save(policy);
    }

    public List<Policy> getPoliciesForUser(UUID userId) {
        return policyRepository.findByUserId(userId);
    }

    public Policy getPolicyById(UUID policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
    }

}