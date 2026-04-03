package com.pfip.userservice.service;

import com.pfip.common.exception.ResourceNotFoundException;
import com.pfip.userservice.dto.AuthDtos;
import com.pfip.userservice.entity.User;
import com.pfip.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AuthDtos.UserDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuthDtos.UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void toggleUserEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        log.info("Toggled enabled status for user {} to {}", user.getUsername(), user.isEnabled());
    }

    private AuthDtos.UserDto toDto(User user) {
        return AuthDtos.UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .build();
    }
}