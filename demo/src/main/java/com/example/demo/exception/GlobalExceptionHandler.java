package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 将备忘录不存在异常转换为 404 响应。 */
    @ExceptionHandler(MemoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleMemoNotFound(MemoNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    /** 将无法解析的请求体转换为 400 响应。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return new ErrorResponse("Invalid request body");
    }

    /** 将团务不存在异常转换为 404 响应。 */
    @ExceptionHandler(CampaignNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCampaignNotFound(CampaignNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    /** 提取首个字段校验错误并返回 400 响应。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidationError(
            MethodArgumentNotValidException exception) {
        var fieldError = exception.getBindingResult().getFieldError();

        return new ValidationErrorResponse(
                "Validation failed",
                fieldError != null ? fieldError.getField() : null,
                fieldError != null ? fieldError.getDefaultMessage() : null);
    }

    public record ErrorResponse(String message) {
    }

    public record ValidationErrorResponse(
            String message,
            String field,
            String error) {
    }

    @ExceptionHandler(CharacterNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCharacterNotFound(
            CharacterNotFoundException exception) {

        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(CampaignMemberNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCampaignMemberNotFound(
            CampaignMemberNotFoundException exception) {

        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBusinessRule(
            BusinessRuleException exception) {

        return new ErrorResponse(exception.getMessage());
    }
}
