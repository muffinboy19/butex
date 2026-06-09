package com.example.butex.controller;

import com.example.butex.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ControllerHelper {

    public ResponseEntity<ApiResponse> sendSuccessResponse(Object data) {
        ApiResponse response = ApiResponse.builder()
                .status(Boolean.TRUE)
                .data(data)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<ApiResponse> sendSuccessResponse(Object data, String message) {
        ApiResponse response = ApiResponse.builder()
                .status(Boolean.TRUE)
                .message(message)
                .data(data)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<ApiResponse> sendSuccessResponse(String message) {
        ApiResponse response = ApiResponse.builder()
                .status(Boolean.TRUE)
                .message(message)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public static ResponseEntity<ApiResponse> sendErrorResponse(HttpStatus httpStatus, String message) {
        return sendErrorResponse(httpStatus, message, null);
    }

    public static ResponseEntity<ApiResponse> sendErrorResponse(HttpStatus httpStatus, String message, Object data) {
        ApiResponse response = ApiResponse.builder()
                .status(Boolean.FALSE)
                .message(message)
                .data(data)
                .build();
        return new ResponseEntity<>(response, httpStatus);
    }
}
