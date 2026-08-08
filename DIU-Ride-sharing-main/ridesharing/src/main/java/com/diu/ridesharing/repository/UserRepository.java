package com.diu.ridesharing.repository;

import com.diu.ridesharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByStudentId(String studentId);
    Optional<User> findByStudentId(String studentId);
    Optional<User> findByResetPasswordToken(String resetPasswordToken);

    Page<User> findByRole(User.Role role, Pageable pageable);
}