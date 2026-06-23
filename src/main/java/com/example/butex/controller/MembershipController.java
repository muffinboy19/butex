package com.example.butex.controller;

import com.example.butex.service.MembershipService;
import com.example.butex.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
}
