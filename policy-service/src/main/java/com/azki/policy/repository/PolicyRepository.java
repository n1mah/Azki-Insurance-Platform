package com.azki.policy.repository;

import com.azki.policy.entity.Policy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    List<Policy> findByUserId(UUID userId);

}