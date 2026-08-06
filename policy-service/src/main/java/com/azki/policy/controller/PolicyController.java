package com.azki.policy.controller;

import com.azki.policy.dto.IssuePolicyRequest;
import com.azki.policy.dto.PolicyResponse;
import com.azki.policy.dto.ProductResponse;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Tag(name = "Policies", description = "Insurance product catalog and policy issuance")
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/products")
    @Operation(summary = "List available insurance products", description = "Cached; returns all products in the catalog")
    public ResponseEntity<List<ProductResponse>> getAvailableProducts() {
        List<ProductResponse> response = policyService.getAvailableProducts().stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Issue a new policy", description = "Creates a policy for the authenticated user against the given product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Policy issued successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed (missing productId)"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<PolicyResponse> issuePolicy(
            @AuthenticationPrincipal String userIdString,
            @Valid @RequestBody IssuePolicyRequest request) {

        UUID userId = UUID.fromString(userIdString);
        Policy policy = policyService.issuePolicy(userId, request.productId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyResponse.from(policy));
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's policies")
    public ResponseEntity<List<PolicyResponse>> getMyPolicies(@AuthenticationPrincipal String userIdString) {
        UUID userId = UUID.fromString(userIdString);
        List<PolicyResponse> response = policyService.getPoliciesForUser(userId).stream()
                .map(PolicyResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{policyId}")
    @Operation(summary = "Get a policy by ID", description = "Returns 404 both when the policy does not exist and when it belongs to a different user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy found"),
            @ApiResponse(responseCode = "404", description = "Policy not found or not owned by the caller")
    })
    public ResponseEntity<PolicyResponse> getPolicy(
            @AuthenticationPrincipal String userIdString,
            @PathVariable UUID policyId) {
        UUID userId = UUID.fromString(userIdString);
        Policy policy = policyService.getPolicyById(policyId, userId);
        return ResponseEntity.ok(PolicyResponse.from(policy));
    }

}