package com.azki.policy.controller;

import com.azki.policy.dto.IssuePolicyRequest;
import com.azki.policy.dto.PolicyResponse;
import com.azki.policy.entity.InsuranceProduct;
import com.azki.policy.entity.Policy;
import com.azki.policy.service.PolicyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<InsuranceProduct>> getAvailableProducts() {
        return ResponseEntity.ok(policyService.getAvailableProducts());
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> issuePolicy(
        @AuthenticationPrincipal String userIdString,
        @Valid @RequestBody IssuePolicyRequest request) {

        UUID userId = UUID.fromString(userIdString);
        Policy policy = policyService.issuePolicy(userId, request.productId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyResponse.from(policy));
    }

    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyResponse> getPolicy(@PathVariable UUID policyId) {
        Policy policy = policyService.getPolicyById(policyId);
        return ResponseEntity.ok(PolicyResponse.from(policy));
    }

}