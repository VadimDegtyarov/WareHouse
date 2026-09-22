package com.kis.wmsapplication.modules.userModule.Exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<AppError> handleResourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("Resource not found: {}", e.getMessage());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AppError> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Invalid argument: {}", e.getMessage());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<AppError> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        log.warn("Invalid state: {}", e.getMessage());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<AppError> handleImageUpload(ImageUploadException e, HttpServletRequest request) {
        log.error("Image upload error: {}", e.getMessage());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppError> handleValidationErrors(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("Validation error: {}", e.getMessage());
        
        List<AppError.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(err -> AppError.FieldError.builder()
                        .field(err.getField())
                        .message(err.getDefaultMessage())
                        .rejectedValue(err.getRejectedValue())
                        .build())
                .collect(Collectors.toList());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("Ошибка валидации данных")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<AppError> handleBindException(BindException e, HttpServletRequest request) {
        log.warn("Binding error: {}", e.getMessage());
        
        List<AppError.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(err -> AppError.FieldError.builder()
                        .field(err.getField())
                        .message(err.getDefaultMessage())
                        .rejectedValue(err.getRejectedValue())
                        .build())
                .collect(Collectors.toList());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("Binding Error")
                .message("Ошибка привязки данных")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<AppError> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        log.warn("Constraint violation: {}", e.getMessage());
        
        List<AppError.FieldError> fieldErrors = e.getConstraintViolations().stream()
                .map(violation -> AppError.FieldError.builder()
                        .field(getFieldName(violation))
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Нарушение ограничений данных")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ============== Ошибки базы данных ==============
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<AppError> handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        log.error("Data integrity violation: {}", e.getMessage());
        
        String message = "Ошибка целостности данных";
        String rootCause = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();
        
        // Определяем тип ошибки по сообщению
        if (rootCause != null) {
            if (rootCause.contains("duplicate key") || rootCause.contains("Duplicate entry") || rootCause.contains("unique constraint")) {
                message = "Запись с такими данными уже существует";
            } else if (rootCause.contains("foreign key") || rootCause.contains("FOREIGN KEY")) {
                message = "Невозможно удалить запись, так как она используется в других данных";
            } else if (rootCause.contains("not-null") || rootCause.contains("NOT NULL")) {
                message = "Обязательное поле не заполнено";
            }
        }
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.CONFLICT.value())
                .error("Data Integrity Violation")
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ============== HTTP ошибки ==============
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AppError> handleMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("Message not readable: {}", e.getMessage());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Некорректный формат данных запроса")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<AppError> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("Missing parameter: {}", e.getParameterName());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Отсутствует обязательный параметр: " + e.getParameterName())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<AppError> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("Type mismatch for parameter: {}", e.getName());
        
        String message = String.format("Параметр '%s' имеет неверный тип. Ожидался: %s", 
                e.getName(), 
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<AppError> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("Method not supported: {}", e.getMethod());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed")
                .message("HTTP метод " + e.getMethod() + " не поддерживается для этого эндпоинта")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<AppError> handleNoHandlerFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("No handler found: {}", e.getRequestURL());
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("Эндпоинт не найден: " + e.getRequestURL())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ============== Общий обработчик ==============
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppError> handleGenericException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error: ", e);
        
        AppError error = AppError.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Внутренняя ошибка сервера. Пожалуйста, попробуйте позже.")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ============== Вспомогательные методы ==============
    
    private String getFieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot + 1) : path;
    }
}