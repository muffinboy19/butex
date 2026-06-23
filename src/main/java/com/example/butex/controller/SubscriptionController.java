package com.example.butex.controller;

import com.example.butex.dto.request.ChangePlanRequest;
import com.example.butex.dto.request.ChangeTierRequest;
import com.example.butex.dto.request.SubscribeRequest;
import com.example.butex.helper.ControllerHelper;
import com.example.butex.service.SubscriptionService;
import com.example.butex.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/users/{userId}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController extends ControllerHelper {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<?> subscribe(@PathVariable Long userId, @Valid @RequestBody SubscribeRequest request) {
        return sendSuccessResponse(subscriptionService.subscribe(userId, request), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSubscription(@PathVariable Long userId) {
        return sendSuccessResponse(subscriptionService.getCurrentSubscription(userId), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @GetMapping("/events")
    public ResponseEntity<?> getSubscriptionActionHistory(@PathVariable Long userId) {
        return sendSuccessResponse(subscriptionService.getSubscriptionActionHistory(userId), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @GetMapping
    public ResponseEntity<?> getSubscriptionHistory(@PathVariable Long userId) {
        return sendSuccessResponse(subscriptionService.getSubscriptionHistory(userId), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(@PathVariable Long userId) {
        return sendSuccessResponse(subscriptionService.cancelSubscription(userId), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @PutMapping("/tier")
    public ResponseEntity<?> changeTier(@PathVariable Long userId, @Valid @RequestBody ChangeTierRequest request) {
        return sendSuccessResponse(subscriptionService.changeTier(userId, request), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @PostMapping("/renew")
    public ResponseEntity<?> renewSubscription(@PathVariable Long userId) {
        return sendSuccessResponse(subscriptionService.renewSubscription(userId), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @PutMapping("/plan")
    public ResponseEntity<?> changePlan(@PathVariable Long userId, @Valid @RequestBody ChangePlanRequest request) {
        return sendSuccessResponse(subscriptionService.changePlan(userId, request), Constants.SUCCESS_STATUS_MESSAGE);
    }
}
