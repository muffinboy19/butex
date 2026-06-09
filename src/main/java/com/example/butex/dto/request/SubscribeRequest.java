package com.example.butex.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscribeRequest {

    @NotNull(message = "planDetailsId is required")
    private Long planDetailsId;

    @NotNull(message = "tierDetailsId is required")
    private Long tierDetailsId;
}
