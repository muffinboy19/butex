package com.example.butex.dto.response;

import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.enums.SubscriptionSubStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionResponse {

    private Long id;
    private Long userId;
    private Long planDetailsId;
    private Long tierDetailsId;
    private String planCode;
    private String planName;
    private String tierCode;
    private String tierName;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private SubscriptionStatus status;
    private SubscriptionSubStatus subStatus;
}
