package com.example.butex.controller;

import com.example.butex.dto.request.CreatePlanDetailsRequest;
import com.example.butex.dto.request.CreateTierDetailsRequest;
import com.example.butex.helper.ControllerHelper;
import com.example.butex.service.MembershipService;
import com.example.butex.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/membership")
@RequiredArgsConstructor
public class MembershipController extends ControllerHelper {

    private final MembershipService membershipService;

    @GetMapping("/plans")
    public ResponseEntity<?> getPlans() {
        return sendSuccessResponse(membershipService.getActivePlans(), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @GetMapping("/tiers")
    public ResponseEntity<?> getTiers() {
        return sendSuccessResponse(membershipService.getActiveTiers(), Constants.SUCCESS_STATUS_MESSAGE);
    }

    @PostMapping("/plans/{planId}/details")
    public ResponseEntity<?> createPlanDetails(@PathVariable Long planId,
                                               @Valid @RequestBody CreatePlanDetailsRequest request) {
        return sendSuccessResponse(membershipService.createPlanDetails(planId, request),
                Constants.SUCCESS_STATUS_MESSAGE);
    }

    @PostMapping("/tiers/{tierId}/details")
    public ResponseEntity<?> createTierDetails(@PathVariable Long tierId,
                                              @Valid @RequestBody CreateTierDetailsRequest request) {
        return sendSuccessResponse(membershipService.createTierDetails(tierId, request),
                Constants.SUCCESS_STATUS_MESSAGE);
    }
}
