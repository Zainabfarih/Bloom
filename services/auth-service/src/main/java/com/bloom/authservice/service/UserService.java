package com.bloom.authservice.service;

import com.bloom.authservice.dto.UserDTO;
import com.bloom.authservice.entity.User;
import com.bloom.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDTO getUserById(Long id) {
        User user = findUserById(id);
        if (!user.isEnabled()) {
            throw new RuntimeException("User account is deleted");
        }
        return toDTO(user);
    }

    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDTO(user);
    }

    public UserDTO recoverUser(Long id) {
        User user = findUserById(id);
        user.setEnabled(true);
        user.setLocked(false);
        return toDTO(userRepository.save(user));
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = findUserById(id);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        return toDTO(userRepository.save(user));
    }

    @Transactional
    public void softDeleteUser(Long id) {
        User user = findUserById(id);
        user.setEnabled(false);
        user.setLocked(true);
        userRepository.save(user);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}