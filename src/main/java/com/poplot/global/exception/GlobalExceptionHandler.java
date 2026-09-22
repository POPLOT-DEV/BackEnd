package com.poplot.global.exception;

import com.poplot.global.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException exception) {
        return createErrorResponse(exception.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<ValidationError> errors = exception.getBindingResult().getAllErrors().stream()
            .map(error -> new ValidationError(error instanceof FieldError fieldError ? fieldError.getField() : "request", error.getDefaultMessage()))
            .toList();
        return createValidationErrorResponse(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleHandlerMethodValidationException(HandlerMethodValidationException exception) {
        if (exception.isForReturnValue()) {
            log.error("Controller return value validation failed", exception);
            return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        List<ValidationError> errors = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result -> {
            String fallback = result.getMethodParameter().getParameterName();
            if (fallback == null) {
                fallback = "arg" + result.getMethodParameter().getParameterIndex();
            }
            String fallbackField = fallback;
            result.getResolvableErrors().forEach(error ->
                errors.add(new ValidationError(error instanceof FieldError fieldError ? fieldError.getField() : fallbackField, error.getDefaultMessage())));
        });
        exception.getCrossParameterValidationResults().forEach(error -> errors.add(new ValidationError("request", error.getDefaultMessage())));
        return createValidationErrorResponse(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleConstraintViolationException(ConstraintViolationException exception) {
        List<ValidationError> errors = exception.getConstraintViolations().stream()
            .map(violation -> new ValidationError(violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();
        return createValidationErrorResponse(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return createErrorResponse(ErrorCode.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        return createErrorResponse(ErrorCode.MISSING_REQUEST_PARAMETER);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        return createErrorResponse(ErrorCode.TYPE_MISMATCH);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        return createErrorResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception) {
        return createErrorResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException exception) {
        return createErrorResponse(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation", exception);
        return createErrorResponse(ErrorCode.DATA_INTEGRITY_VIOLATION);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> createErrorResponse(BaseErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.failure(errorCode.getCode(), errorCode.getMessage()));
    }

    private ResponseEntity<ApiResponse<List<ValidationError>>> createValidationErrorResponse(List<ValidationError> errors) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.failure(errorCode.getCode(), errorCode.getMessage(), errors));
    }
}
