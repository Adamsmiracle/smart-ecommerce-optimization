package com.miracle.smart_ecommerce_security.exception;

import java.time.OffsetDateTime;

public class ApiError {
    private boolean success = false;
    private ErrorCode errorCode;
    private String message;
    private String detail;
    private String path;
    private String correlationId;
    private String clientIp;
    private OffsetDateTime timestamp = OffsetDateTime.now();

    public ApiError() {}

    public ApiError(ErrorCode errorCode, String message, String detail, String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.detail = detail;
        this.path = path;
    }

    public ApiError(ErrorCode errorCode, String message, String detail, String path, String correlationId, String clientIp) {
        this.errorCode = errorCode;
        this.message = message;
        this.detail = detail;
        this.path = path;
        this.correlationId = correlationId;
        this.clientIp = clientIp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ApiError{" +
                "errorCode=" + errorCode +
                ", message='" + message + '\'' +
                ", detail='" + detail + '\'' +
                ", path='" + path + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
