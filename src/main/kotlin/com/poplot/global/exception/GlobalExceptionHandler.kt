package com.poplot.global.exception

import com.poplot.global.common.response.ApiResponse
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(GeneralException::class)
    fun handleGeneralException(exception: GeneralException): ResponseEntity<ApiResponse<Void>> =
        createErrorResponse(exception.errorCode)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        exception: MethodArgumentNotValidException,
    ): ResponseEntity<ApiResponse<List<ValidationError>>> {
        val errors = exception.bindingResult.allErrors.map { error ->
            ValidationError(
                field = (error as? FieldError)?.field ?: "request",
                message = error.defaultMessage,
            )
        }
        return createValidationErrorResponse(errors)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(exception: HandlerMethodValidationException): ResponseEntity<*> {
        if (exception.isForReturnValue) {
            log.error("Controller return value validation failed", exception)
            return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR)
        }

        val parameterErrors = exception.parameterValidationResults.flatMap { result ->
            val parameter = result.methodParameter
            val fallbackField = parameter.parameterName ?: "arg${parameter.parameterIndex}"
            result.resolvableErrors.map { error ->
                ValidationError(
                    field = (error as? FieldError)?.field ?: fallbackField,
                    message = error.defaultMessage,
                )
            }
        }
        val crossParameterErrors = exception.crossParameterValidationResults.map { error ->
            ValidationError("request", error.defaultMessage)
        }

        return createValidationErrorResponse(parameterErrors + crossParameterErrors)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        exception: ConstraintViolationException,
    ): ResponseEntity<ApiResponse<List<ValidationError>>> =
        createValidationErrorResponse(
            exception.constraintViolations.map { violation ->
                ValidationError(violation.propertyPath.toString(), violation.message)
            },
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException) =
        createErrorResponse(ErrorCode.INVALID_REQUEST_BODY)

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(exception: MissingServletRequestParameterException) =
        createErrorResponse(ErrorCode.MISSING_REQUEST_PARAMETER)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(exception: MethodArgumentTypeMismatchException) =
        createErrorResponse(ErrorCode.TYPE_MISMATCH)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(exception: HttpRequestMethodNotSupportedException) =
        createErrorResponse(ErrorCode.METHOD_NOT_ALLOWED)

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupportedException(exception: HttpMediaTypeNotSupportedException) =
        createErrorResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE)

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(exception: NoResourceFoundException) =
        createErrorResponse(ErrorCode.RESOURCE_NOT_FOUND)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(exception: DataIntegrityViolationException): ResponseEntity<ApiResponse<Void>> {
        log.warn("Data integrity violation", exception)
        return createErrorResponse(ErrorCode.DATA_INTEGRITY_VIOLATION)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ApiResponse<Void>> {
        log.error("Unhandled exception", exception)
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    private fun createErrorResponse(errorCode: BaseErrorCode): ResponseEntity<ApiResponse<Void>> =
        ResponseEntity.status(errorCode.httpStatus)
            .body(ApiResponse.failure(errorCode.code, errorCode.message))

    private fun createValidationErrorResponse(
        errors: List<ValidationError>,
    ): ResponseEntity<ApiResponse<List<ValidationError>>> {
        val errorCode = ErrorCode.INVALID_INPUT_VALUE
        return ResponseEntity.status(errorCode.httpStatus)
            .body(ApiResponse.failure(errorCode.code, errorCode.message, errors))
    }

    private companion object {
        val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
