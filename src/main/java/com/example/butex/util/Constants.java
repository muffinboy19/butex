package com.example.butex.util;

public final class Constants {

    public static final String SUCCESSFUL_STATUS_MESSAGE = "Success";
    public static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    public static final String MALFORMED_REQUEST_MESSAGE = "Malformed request body";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "Something went wrong";
    public static final String METHOD_NOT_ALLOWED_MESSAGE = "Method not allowed";
    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";

    /** Every day at 1:00 AM — expire overdue subscriptions */
    public static final String SUBSCRIPTION_EXPIRY_CRON = "0 0 1 * * *";

    /** Every day at 2:00 AM — promote users to higher qualifying tiers */
    public static final String TIER_PROMOTION_CRON = "0 0 2 * * *";

    public static final String SUBSCRIPTION_EXPIRY_LOCK_PREFIX = "subscription-expiry-cron:";
    public static final String TIER_PROMOTION_LOCK_PREFIX = "tier-promotion-cron:";
    public static final String SUBSCRIPTION_USER_LOCK_PREFIX = "subscription-user:";

    private Constants() {
    }
}
