package com.diu.ridesharing.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String m){ super(m); }
}