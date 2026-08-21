package com.saibao.invoice.controller;

import com.saibao.invoice.exception.QrTokenExpiredException;
import com.saibao.invoice.vo.ApiErrorVO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将可预期业务错误转换为稳定的 HTTP 状态码与中文说明。 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(QrTokenExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ApiErrorVO handleQrTokenExpired(QrTokenExpiredException exception) {
        return new ApiErrorVO(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorVO handleBadRequest(IllegalArgumentException exception) {
        return new ApiErrorVO(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorVO handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().isEmpty()
                ? "请求参数不合法"
                : exception.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return new ApiErrorVO(message);
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorVO handleForbidden(SecurityException exception) {
        return new ApiErrorVO(exception.getMessage());
    }
}
