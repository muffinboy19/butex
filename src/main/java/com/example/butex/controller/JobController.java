package com.example.butex.controller;

import com.example.butex.helper.ControllerHelper;
import com.example.butex.service.scheduler.SchedulerService;
import com.example.butex.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/admin/jobs")
@RequiredArgsConstructor
public class JobController extends ControllerHelper {

    private final SchedulerService schedulerService;

    @PostMapping("/subscription-expiry")
    public ResponseEntity<?> triggerSubscriptionExpiry() {
        return sendSuccessResponse(schedulerService.runSubscriptionExpiryJob(), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @PostMapping("/tier-promotion")
    public ResponseEntity<?> triggerTierPromotion() {
        return sendSuccessResponse(schedulerService.runTierPromotionJob(), Constants.SUCCESS_STATUS_MESSAGE);
    }
}
