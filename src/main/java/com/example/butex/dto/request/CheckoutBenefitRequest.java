package com.example.butex.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CheckoutBenefitRequest {

    @NotEmpty(message = "items are required")
    @Valid
    private List<CheckoutLineItem> items;

    @Getter
    @Setter
    public static class CheckoutLineItem {

        @NotNull(message = "itemId is required")
        private String itemId;

        @NotNull(message = "categoryId is required")
        private String categoryId;

        @NotNull(message = "lineTotal is required")
        @Positive(message = "lineTotal must be positive")
        private BigDecimal lineTotal;
    }
}
