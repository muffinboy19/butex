package com.example.butex.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TierResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer rank;
    private List<TierDetailsResponse> activeDetails;
}
