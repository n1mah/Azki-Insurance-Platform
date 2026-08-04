package com.azki.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.azki.policy.entity.InsuranceProduct;
import com.azki.policy.entity.Policy;
import com.azki.policy.entity.PolicyStatus;
import com.azki.policy.exception.PolicyNotFoundException;
import com.azki.policy.exception.ProductNotFoundException;
import com.azki.policy.repository.InsuranceProductRepository;
import com.azki.policy.repository.PolicyRepository;
import com.azki.policy.valueobject.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private InsuranceProductRepository productRepository;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyRepository, productRepository);
    }

    @Test
    void shouldIssuePolicyWithPremiumFromProductAndActiveStatus() {
        // given
        UUID userId = UUID.randomUUID();
        Long productId = 1L;
        InsuranceProduct product = new InsuranceProduct(
                "Car Body Insurance", Money.of(new BigDecimal("1500.00")), "COMPREHENSIVE");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Policy result = policyService.issuePolicy(userId, productId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getProduct()).isEqualTo(product);
        assertThat(result.getPremiumAmount().getAmount()).isEqualByComparingTo("1500.00");
        assertThat(result.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
    }

    @Test
    void shouldThrowExceptionWhenIssuingPolicyForNonExistentProduct() {
        // given
        UUID userId = UUID.randomUUID();
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> policyService.issuePolicy(userId, productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldReturnPoliciesForGivenUser() {
        // given
        UUID userId = UUID.randomUUID();
        InsuranceProduct product = new InsuranceProduct(
                "Car Body Insurance", Money.of(new BigDecimal("1500.00")), "COMPREHENSIVE");
        Policy policy = new Policy(userId, product, Money.of(new BigDecimal("1500.00")),
                LocalDate.now(), LocalDate.now().plusYears(1));

        when(policyRepository.findByUserId(userId)).thenReturn(List.of(policy));

        // when
        List<Policy> result = policyService.getPoliciesForUser(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldReturnPolicyWhenFoundById() {
        // given
        UUID policyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        InsuranceProduct product = new InsuranceProduct(
                "Car Body Insurance", Money.of(new BigDecimal("1500.00")), "COMPREHENSIVE");
        Policy policy = new Policy(userId, product, Money.of(new BigDecimal("1500.00")),
                LocalDate.now(), LocalDate.now().plusYears(1));

        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));

        // when
        Policy result = policyService.getPolicyById(policyId);

        // then
        assertThat(result).isEqualTo(policy);
    }

    @Test
    void shouldThrowExceptionWhenPolicyNotFoundById() {
        // given
        UUID policyId = UUID.randomUUID();
        when(policyRepository.findById(policyId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> policyService.getPolicyById(policyId))
                .isInstanceOf(PolicyNotFoundException.class);
    }

}