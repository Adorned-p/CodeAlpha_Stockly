package com.codealpha.stockly.exception;

import com.codealpha.stockly.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(
            MethodArgumentNotValidException exception
    ) {

        String message =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .orElse("Invalid request");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleInvalidJson(
            HttpMessageNotReadableException exception
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request body"
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ErrorResponse handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: "
                        + exception.getName()
        );
    }

    @ExceptionHandler(MarketDataException.class)
    public ErrorResponse handleMarketDataException(
            MarketDataException exception
    ) {

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResponse handleResourceNotFoundException(
            ResourceNotFoundException exception
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(
            Exception exception
    ) {

        // Log the real exception so it appears in the Spring Boot console.
        System.err.println(
                "========== UNHANDLED EXCEPTION =========="
        );

        exception.printStackTrace();

        System.err.println(
                "=========================================="
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
    }

    private ErrorResponse buildResponse(
            HttpStatus status,
            String message
    ) {

        return new ErrorResponse(
                status.value(),
                message,
                LocalDateTime.now()
        );
    }
}