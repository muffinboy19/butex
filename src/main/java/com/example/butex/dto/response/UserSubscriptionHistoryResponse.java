package com.example.butex.dto.response;

import com.example.butex.enums.UserSubscriptionHistoryAction;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserSubscriptionHistoryResponse {

    private Long id;
    private Long userId;
    private Long subscriptionId;
    private UserSubscriptionHistoryAction action;
    private String remark;
    private String actionBy;
    private LocalDateTime actionAt;
}
