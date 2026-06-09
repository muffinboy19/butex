package com.example.butex.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private List<PlanDetailsResponse> activeDetails;
}
