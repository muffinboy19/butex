package com.example.butex.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlanResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private List<PlanDetailsResponse> activeDetails;
}
