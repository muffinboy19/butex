package com.example.butex.exception;

import com.example.butex.controller.ControllerHelper;
import com.example.butex.dto.response.ApiResponse;
import com.example.butex.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleNotFound(ResourceNotFoundException ex) {
        return ControllerHelper.sendErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({BusinessException.class, InvalidRequestException.class})
    public ResponseEntity<ApiResponse> handleBusiness(RuntimeException ex) {
        return ControllerHelper.sendErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(LockException.class)
    public ResponseEntity<ApiResponse> handleLock(LockException ex) {
        return ControllerHelper.sendErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ControllerHelper.sendErrorResponse(HttpStatus.BAD_REQUEST, Constants.INVALID_REQUEST_MESSAGE, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
        return ControllerHelper.sendErrorResponse(HttpStatus.BAD_REQUEST, Constants.MALFORMED_REQUEST_MESSAGE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = "Missing required parameter: " + ex.getParameterName();
        return ControllerHelper.sendErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter: " + ex.getName();
        return ControllerHelper.sendErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ControllerHelper.sendErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, Constants.METHOD_NOT_ALLOWED_MESSAGE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse> handleNoResource(NoResourceFoundException ex) {
        return ControllerHelper.sendErrorResponse(HttpStatus.NOT_FOUND, Constants.RESOURCE_NOT_FOUND_MESSAGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ControllerHelper.sendErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, Constants.INTERNAL_SERVER_ERROR_MESSAGE);
    }
}
