package com.hcl.bankease.service;

import com.hcl.bankease.dto.RegisterRequest;
import com.hcl.bankease.entity.Account;
import com.hcl.bankease.entity.User;
import com.hcl.bankease.exception.DuplicateResourceException;
import com.hcl.bankease.exception.ResourceNotFoundException;
import com.hcl.bankease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("Phone number already registered: " + request.getPhoneNumber());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.CUSTOMER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        // Every new customer gets a default savings account automatically
        accountService.createAccountForUser(savedUser, Account.AccountType.SAVINGS);

        return savedUser;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    public void assertNotLocked(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
                throw new org.springframework.security.authentication.BadCredentialsException(
                        "Account locked for 15 minutes due to failed logins");
            }
        });
    }

    @Transactional
    public void recordFailure(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setFailedAttempts(u.getFailedAttempts() + 1);
            if (u.getFailedAttempts() >= 5) {
                u.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(15));
                u.setFailedAttempts(0);
            }
            userRepository.save(u);
        });
    }

    @Transactional
    public void recordSuccess(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
            userRepository.save(u);
        });
    }

    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}