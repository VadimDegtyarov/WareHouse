package com.kis.wmsapplication.modules.userModule.Exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppError {
    private int statusCode;
    private String error;
    private String message;
    private String path;
    private Instant timestamp;
    private List<FieldError> fieldErrors;
    private Map<String, Object> details;

    public AppError(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public AppError(int statusCode, String error, String message, String path,
                   Instant timestamp, List<FieldError> fieldErrors, Map<String, Object> details) {
        this.statusCode = statusCode;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.fieldErrors = fieldErrors;
        this.details = details;
    }

    @Getter
    @Setter
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
