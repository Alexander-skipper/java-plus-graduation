package ru.practicum.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NoSuchElementException e) {
        log.warn("NotFoundException: {}", e.getMessage(), e);
        return ApiError.builder()
                .status("NOT_FOUND")
                .reason("The required object was not found.")
                .message(e.getMessage())
                .errors(List.of())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            IllegalStateException.class,
            ConflictException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(RuntimeException e) {
        log.error("ConflictException: {}", e.getMessage(), e);
        return ApiError.builder()
                .status("CONFLICT")
                .reason("Integrity constraint has been violated.")
                .message(e.getMessage())
                .errors(List.of())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            DateTimeParseException.class,
            HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(Exception e) {
        log.warn("BadRequestException: {}", e.getMessage(), e);

        String message = e.getMessage();
        if (e instanceof DateTimeParseException) {
            message = "Invalid date format. Expected format: yyyy-MM-dd HH:mm:ss";
        } else if (e instanceof MethodArgumentTypeMismatchException) {
            message = "Failed to convert value to required type";
        } else if (e instanceof MissingServletRequestParameterException) {
            message = "Required request parameter '" + ((MissingServletRequestParameterException) e).getParameterName() + "' is not present";
        }

        return ApiError.builder()
                .status("BAD_REQUEST")
                .reason("Incorrectly made request.")
                .message(message)
                .errors(List.of())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ResourceAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbiddenRequest(ResourceAccessException e) {
        log.warn("Forbidden: {}", e.getMessage(), e);
        return ApiError.builder()
                .status("FORBIDDEN")
                .reason("Access denied.")
                .message(e.getMessage())
                .errors(List.of())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGenericException(Exception e) {
        log.error("Internal server error: {}", e.getMessage(), e);
        return ApiError.builder()
                .status("INTERNAL_SERVER_ERROR")
                .reason("Internal server error")
                .message(e.getMessage())
                .errors(List.of())
                .timestamp(LocalDateTime.now())
                .build();
    }


}
