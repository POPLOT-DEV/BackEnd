package com.poplot.feature.user.repository;

import com.poplot.feature.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPublicId(UUID publicId);
}
