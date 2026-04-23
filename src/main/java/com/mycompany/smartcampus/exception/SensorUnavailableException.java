package com.mycompany.smartcampus.exception;

 //Thrown when a POST reading is attempted on a sensor that is in

public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String message) {
        super(message);
    }
}
