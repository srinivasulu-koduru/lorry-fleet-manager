package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    List<User> findByRole(Role role);

    List<User> findByRoleAndIsActive(Role role, boolean isActive);
}
