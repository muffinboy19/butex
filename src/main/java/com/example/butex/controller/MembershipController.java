package com.example.butex.controller;

import com.example.butex.dto.request.CreatePlanDetailsRequest;
import com.example.butex.dto.request.CreateTierDetailsRequest;
import com.example.butex.dto.request.UpdatePlanDetailsRequest;
import com.example.butex.dto.request.UpdateTierDetailsRequest;
import com.example.butex.helper.ControllerHelper;
import com.example.butex.service.MembershipService;
import com.example.butex.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PutMapping("/plans/{planId}/details/{detailsId}")
    public ResponseEntity<?> updatePlanDetails(@PathVariable Long planId,
                                               @PathVariable Long detailsId,
                                               @Valid @RequestBody UpdatePlanDetailsRequest request) {
        return sendSuccessResponse(membershipService.updatePlanDetails(planId, detailsId, request),
                Constants.SUCCESS_STATUS_MESSAGE);
    }

    @PutMapping("/tiers/{tierId}/details/{detailsId}")
    public ResponseEntity<?> updateTierDetails(@PathVariable Long tierId,
                                               @PathVariable Long detailsId,
                                               @Valid @RequestBody UpdateTierDetailsRequest request) {
        return sendSuccessResponse(membershipService.updateTierDetails(tierId, detailsId, request),
                Constants.SUCCESS_STATUS_MESSAGE);
    }

    @DeleteMapping("/plans/{planId}/details/{detailsId}")
    public ResponseEntity<?> deactivatePlanDetails(@PathVariable Long planId,
                                                   @PathVariable Long detailsId) {
        return sendSuccessResponse(membershipService.deactivatePlanDetails(planId, detailsId),
                Constants.SUCCESS_STATUS_MESSAGE);
    }

    @DeleteMapping("/tiers/{tierId}/details/{detailsId}")
    public ResponseEntity<?> deactivateTierDetails(@PathVariable Long tierId,
                                                   @PathVariable Long detailsId) {
        return sendSuccessResponse(membershipService.deactivateTierDetails(tierId, detailsId),
                Constants.SUCCESS_STATUS_MESSAGE);
    }
}
