package com.bctech.cashshareapplication.core.exceptions;

import com.bctech.cashshareapplication.model.enums.ResponseStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
public class InsufficientBalanceException extends RuntimeException{
    protected String message;
    protected HttpStatus status;
    protected final String statusCode = ResponseStatus.INSUFFICIENT_FUNDS.getCode();


    public InsufficientBalanceException(String message) {
        super(message);
        this.message = message;
    }

    public InsufficientBalanceException(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }
}
