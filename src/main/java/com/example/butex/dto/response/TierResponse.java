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
public class TierResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer rank;
    private List<TierDetailsResponse> activeDetails;
}
