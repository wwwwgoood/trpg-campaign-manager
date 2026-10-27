package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessRuleException extends RuntimeException{
    /** 使用具体的业务规则错误信息构造异常。 */
    public BusinessRuleException(String message)
    {
        super(message);
    }
}
