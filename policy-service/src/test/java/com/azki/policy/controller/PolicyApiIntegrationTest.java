package com.azki.policy.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azki.policy.entity.InsuranceProduct;
import com.azki.policy.repository.InsuranceProductRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.azki.policy.valueobject.Money;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PolicyApiIntegrationTest {

        private static final String TEST_SECRET = "CHANGE_ME_IN_PRODUCTION_MIN_256_BITS_LONG_SECRET_KEY";

        @Container
        static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
                        .withDatabaseName("policy_db_test")
                        .withUsername("test_user")
                        .withPassword("test_password");

        @Container
        static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                        .withExposedPorts(6379);

        @DynamicPropertySource
        static void overrideProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", mysql::getJdbcUrl);
                registry.add("spring.datasource.username", mysql::getUsername);
                registry.add("spring.datasource.password", mysql::getPassword);
                registry.add("spring.data.redis.host", redis::getHost);
                registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        }

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private InsuranceProductRepository productRepository;

        private Long existingProductId;

        @BeforeEach
        void setUp() {
                InsuranceProduct product = productRepository.save(
                                new InsuranceProduct("Car Body Insurance", Money.of(new BigDecimal("1500.00")),
                                                "COMPREHENSIVE"));
                existingProductId = product.getId();
        }

        private String generateTestToken(UUID userId) {
                Key signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
                Date now = new Date();
                Date expiry = new Date(now.getTime() + 3_600_000L);

                return Jwts.builder()
                                .subject(userId.toString())
                                .claim("username", "integration_test_user")
                                .claim("role", "CUSTOMER")
                                .issuedAt(now)
                                .expiration(expiry)
                                .signWith(signingKey)
                                .compact();
        }

        @Test
        void shouldRejectRequestWithoutToken() throws Exception {
                mockMvc.perform(get("/api/policies/products"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnAvailableProductsWithValidToken() throws Exception {
                String token = generateTestToken(UUID.randomUUID());

                mockMvc.perform(get("/api/policies/products")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Car Body Insurance"));
        }

        @Test
        void shouldIssuePolicyAndRetrieveIt() throws Exception {
                String token = generateTestToken(UUID.randomUUID());
                String requestBody = "{\"productId\": " + existingProductId + "}";

                String responseBody = mockMvc.perform(post("/api/policies")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id", notNullValue()))
                                .andExpect(jsonPath("$.status").value("ACTIVE"))
                                .andReturn().getResponse().getContentAsString();

                String policyId = responseBody.split("\"id\":\"")[1].split("\"")[0];

                mockMvc.perform(get("/api/policies/" + policyId)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.premiumAmount").value(1500.00));
        }

        @Test
        void shouldReturnNotFoundWhenPolicyBelongsToAnotherUser() throws Exception {
                String ownerToken = generateTestToken(UUID.randomUUID());
                String requestBody = "{\"productId\": " + existingProductId + "}";

                String responseBody = mockMvc.perform(post("/api/policies")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                String policyId = responseBody.split("\"id\":\"")[1].split("\"")[0];
                String otherUserToken = generateTestToken(UUID.randomUUID());

                mockMvc.perform(get("/api/policies/" + policyId)
                                .header("Authorization", "Bearer " + otherUserToken))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnNotFoundForNonExistentProduct() throws Exception {
                String token = generateTestToken(UUID.randomUUID());
                String requestBody = "{\"productId\": 999999}";

                mockMvc.perform(post("/api/policies")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnNotFoundForNonExistentPolicy() throws Exception {
                String token = generateTestToken(UUID.randomUUID());
                String randomPolicyId = UUID.randomUUID().toString();

                mockMvc.perform(get("/api/policies/" + randomPolicyId)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

}