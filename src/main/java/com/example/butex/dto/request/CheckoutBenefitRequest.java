package com.example.butex.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CheckoutBenefitRequest {

    @NotEmpty(message = "items are required")
    @Valid
    private List<CheckoutLineItemRequest> items;
}
