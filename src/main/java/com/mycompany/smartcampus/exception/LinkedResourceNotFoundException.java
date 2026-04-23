package com.mycompany.smartcampus.exception;

 //Thrown when a request references a resource that does not exist

public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
