package com.bloom.authservice.service;

import com.bloom.authservice.dto.UserDTO;
import com.bloom.authservice.entity.Role;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.mapper.UserMapper; // Import the mapper
import com.bloom.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.bloom.authservice.dto.AdminStatsResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    public UserDTO getUserById(Long id) {
        checkOwnership(id);
        User user = findUserById(id);
        if (!user.isEnabled()) {
            throw new RuntimeException("User account is deleted");
        }
        return userMapper.toDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        checkOwnership(id);
        User user = findUserById(id);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public void softDeleteUser(Long id) {
        checkOwnership(id);
        User user = findUserById(id);
        user.setEnabled(false);
        user.setLocked(true);
        user.setDeleted(true);
        refreshTokenService.revokeByUserId(id);
        userRepository.save(user);
    }

    private void checkOwnership(Long id) {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (!currentUser.getId().equals(id) && !currentUser.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    @Transactional
    public void recoverUser(Long id) {
        // findByIdIncludingDeleted bypasses @SQLRestriction so soft-deleted users can be restored
        userRepository.findByIdIncludingDeleted(id).ifPresentOrElse(u -> {
            if (u.isEnabled() && !u.isDeleted()) {
                throw new IllegalStateException("User is already active");
            }
            u.setEnabled(true);
            u.setLocked(false);
            u.setDeleted(false);
            u.setFailedLoginAttempts(0);
            userRepository.save(u);
        }, () -> { throw new RuntimeException("User not found"); });
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        requireAdmin();
        // Include soft-deleted users so the admin can see status and restore them
        return userRepository.findAllIncludingDeleted().stream()
                .map(userMapper::toAdminDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        requireAdmin();

        long total   = userRepository.countAllIncludingDeleted();
        long active  = userRepository.countActive();
        long deleted = userRepository.countDeleted();
        long admins  = userRepository.countByRoleActive(Role.ADMIN.name());
        long students = userRepository.countByRoleActive(Role.STUDENT.name());

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long newThisMonth = userRepository.countCreatedSince(startOfMonth);

        return AdminStatsResponse.builder()
                .totalUsers(total)
                .activeUsers(active)
                .deletedUsers(deleted)
                .newUsersThisMonth(newThisMonth)
                .adminCount(admins)
                .studentCount(students)
                .signupsByDay(buildSignupTrend())
                .build();
    }

    /** Sign-ups per day for the last 30 days, DB-agnostic (computed in-memory). */
    private List<AdminStatsResponse.DailyCount> buildSignupTrend() {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);

        // Seed every day with 0 so the chart has no gaps
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            counts.put(start.plusDays(i).format(fmt), 0L);
        }

        userRepository.findAllIncludingDeleted().forEach(u -> {
            if (u.getCreatedAt() == null) return;
            LocalDate day = u.getCreatedAt().toLocalDate();
            if (!day.isBefore(start) && !day.isAfter(today)) {
                counts.merge(day.format(fmt), 1L, Long::sum);
            }
        });

        List<AdminStatsResponse.DailyCount> trend = new ArrayList<>();
        counts.forEach((date, count) -> trend.add(new AdminStatsResponse.DailyCount(date, count)));
        return trend;
    }

    private void requireAdmin() {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Access denied");
        }
    }

}