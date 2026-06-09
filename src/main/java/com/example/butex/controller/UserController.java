package com.example.butex.controller;

import com.example.butex.dto.request.CreateUserRequest;
import com.example.butex.service.UserService;
import com.example.butex.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController extends ControllerHelper {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        return sendSuccessResponse(userService.createUser(request), Constants.SUCCESSFUL_STATUS_MESSAGE);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        return sendSuccessResponse(userService.getUser(userId), Constants.SUCCESSFUL_STATUS_MESSAGE);
    }
}
