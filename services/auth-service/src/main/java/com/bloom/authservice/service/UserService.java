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
        userRepository.findById(id).ifPresentOrElse(u -> {
            if (u.isEnabled()) {
                throw new IllegalStateException("User is already active");
            }
            u.setEnabled(true);
            u.setLocked(false);
            u.setDeleted(false);
            u.setFailedLoginAttempts(0);
            userRepository.save(u);
        }, () -> { throw new RuntimeException("User not found"); });
    }
}