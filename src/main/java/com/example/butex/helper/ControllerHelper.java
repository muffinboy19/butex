package com.example.butex.helper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ControllerHelper {

    public ResponseEntity<ApiResponseHelper> sendSuccessResponse(Object data) {
        ApiResponseHelper response = ApiResponseHelper.builder()
                .status(Boolean.TRUE)
                .data(data)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<ApiResponseHelper> sendSuccessResponse(Object data, String message) {
        ApiResponseHelper response = ApiResponseHelper.builder()
                .status(Boolean.TRUE)
                .message(message)
                .data(data)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<ApiResponseHelper> sendSuccessResponse(String message) {
        ApiResponseHelper response = ApiResponseHelper.builder()
                .status(Boolean.TRUE)
                .message(message)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public static ResponseEntity<ApiResponseHelper> sendErrorResponse(HttpStatus httpStatus, String message) {
        return sendErrorResponse(httpStatus, message, null);
    }

    public static ResponseEntity<ApiResponseHelper> sendErrorResponse(HttpStatus httpStatus, String message, Object data) {
        ApiResponseHelper response = ApiResponseHelper.builder()
                .status(Boolean.FALSE)
                .message(message)
                .data(data)
                .build();
        return new ResponseEntity<>(response, httpStatus);
    }
}
