package com.example.butex.service;

import com.example.butex.entity.User;
import com.example.butex.dto.request.CreateUserRequest;
import com.example.butex.dto.response.UserResponse;
import com.example.butex.exception.BusinessException;
import com.example.butex.exception.ResourceNotFoundException;
import com.example.butex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        validateCreateRequest(request);
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .cohortCode(request.getCohortCode())
                .build();

        User saved = userRepository.save(user);
        log.info("Created user id={} email={}", saved.getId(), saved.getEmail());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(findUser(userId));
    }

    public User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void validateCreateRequest(CreateUserRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("Name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessException("Email is required");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new BusinessException("Phone is required");
        }
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .active(user.isActive())
                .cohortCode(user.getCohortCode())
                .build();
    }
}
