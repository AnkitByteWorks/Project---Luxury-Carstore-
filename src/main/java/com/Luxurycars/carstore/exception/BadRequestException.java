package com.Luxurycars.carstore.exception;



public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
