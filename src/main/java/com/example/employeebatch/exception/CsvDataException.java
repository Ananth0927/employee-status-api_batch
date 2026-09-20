package com.example.employeebatch.exception;

//error handling...
public class CsvDataException extends RuntimeException {
    public CsvDataException(String message) {
        super(message);
    }
}
