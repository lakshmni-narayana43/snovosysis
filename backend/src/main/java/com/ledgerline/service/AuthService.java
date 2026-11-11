package com.ledgerline.service;

import com.ledgerline.dto.AuthResponse;
import com.ledgerline.dto.CreateAccountRequest;
import com.ledgerline.dto.LoginRequest;
import com.ledgerline.dto.RegisterRequest;
import com.ledgerline.dto.UserDto;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.AuditAction;
import com.ledgerline.enums.AuditStatus;
import com.ledgerline.enums.Role;
import com.ledgerline.enums.UserStatus;
import com.ledgerline.exception.ForbiddenException;
import com.ledgerline.exception.InvalidTransactionException;
import com.ledgerline.exception.ResourceNotFoundException;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;
    private final AccountService accountService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       AuthenticationManager authenticationManager,
                       AuditService auditService,
                       AccountService accountService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
        this.accountService = accountService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new InvalidTransactionException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new InvalidTransactionException("Email '" + request.getEmail() + "' is already in use");
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.ROLE_ACCOUNT_HOLDER;

        User user = new User(
                request.getUsername().trim(),
                request.getEmail().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                assignedRole
        );

        User savedUser = userRepository.save(user);

        // Auto-provision default primary account for account holders
        if (assignedRole == Role.ROLE_ACCOUNT_HOLDER) {
            accountService.createAccount(
                    savedUser.getId(),
                    new CreateAccountRequest(AccountType.SAVINGS, "INR", BigDecimal.ZERO)
            );
        }

        auditService.recordAudit(
                savedUser.getId(),
                savedUser.getUsername(),
                AuditAction.USER_REGISTER,
                "USER",
                savedUser.getId().toString(),
                AuditStatus.SUCCESS,
                null,
                null,
                "Registered user with role " + assignedRole
        );

        String token = jwtUtils.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getRole().name());
        return new AuthResponse(token, new UserDto(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(request.getUsername().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new ForbiddenException("Account is suspended. Please contact administrator.");
            }

            String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole().name());

            auditService.recordAudit(
                    user.getId(),
                    user.getUsername(),
                    AuditAction.USER_LOGIN,
                    "USER",
                    user.getId().toString(),
                    AuditStatus.SUCCESS,
                    null,
                    null,
                    "Successful authentication"
            );

            return new AuthResponse(token, new UserDto(user));
        } catch (Exception e) {
            auditService.recordAudit(
                    null,
                    request.getUsername(),
                    AuditAction.LOGIN_FAILED,
                    "USER",
                    request.getUsername(),
                    AuditStatus.FAILURE,
                    null,
                    e.getMessage(),
                    "Failed authentication attempt"
            );
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return new UserDto(user);
    }
}

