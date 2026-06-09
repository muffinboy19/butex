package com.example.butex.controller;

import com.example.butex.service.MembershipCatalogService;
import com.example.butex.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/membership")
@RequiredArgsConstructor
public class MembershipCatalogController extends ControllerHelper {

    private final MembershipCatalogService catalogService;

    @GetMapping("/plans")
    public ResponseEntity<?> getPlans() {
        return sendSuccessResponse(catalogService.getActivePlans(), Constants.SUCCESSFUL_STATUS_MESSAGE);
    }

    @GetMapping("/tiers")
    public ResponseEntity<?> getTiers() {
        return sendSuccessResponse(catalogService.getActiveTiers(), Constants.SUCCESSFUL_STATUS_MESSAGE);
    }
}
