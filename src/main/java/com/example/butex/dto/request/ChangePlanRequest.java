package com.example.butex.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePlanRequest {

    @NotNull(message = "planDetailsId is required")
    private Long planDetailsId;
}
