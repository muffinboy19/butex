package com.example.butex.controller;

import com.example.butex.dto.request.CheckoutBenefitRequest;
import com.example.butex.helper.ControllerHelper;
import com.example.butex.service.CheckoutBenefitService;
import com.example.butex.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/users/{userId}/checkout")
@RequiredArgsConstructor
public class CheckoutController extends ControllerHelper {

    private final CheckoutBenefitService checkoutBenefitService;

    @PostMapping("/benefits")
    public ResponseEntity<?> calculateBenefits(@PathVariable Long userId,
                                               @Valid @RequestBody CheckoutBenefitRequest request) {
        return sendSuccessResponse(
                checkoutBenefitService.calculateBenefits(userId, request),
                Constants.SUCCESS_STATUS_MESSAGE);
    }
}
