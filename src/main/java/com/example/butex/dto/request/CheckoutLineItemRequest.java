package com.example.butex.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CheckoutLineItemRequest {

    @NotNull(message = "itemId is required")
    private String itemId;

    @NotNull(message = "categoryId is required")
    private String categoryId;

    @NotNull(message = "lineTotal is required")
    private BigDecimal lineTotal;
}
