package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<User> findByRoleAndOwner(Role role, User owner);

    @Query("SELECT u FROM User u WHERE u.role = :role AND (u.owner = :owner OR u.owner IS NULL)")
    List<User> findDriversForOwner(@Param("role") Role role, @Param("owner") User owner);

    Optional<User> findByAssignedLorry(Lorry lorry);

    Optional<User> findByAssignedLorryId(Long lorryId);

    Optional<User> findByIdAndRole(Long id, Role role);
}
