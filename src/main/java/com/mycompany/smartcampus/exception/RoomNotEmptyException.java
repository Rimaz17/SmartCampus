package com.mycompany.smartcampus.exception;

 //Thrown when a DELETE is attempted on a room that still has
 
public class RoomNotEmptyException extends RuntimeException {

    public RoomNotEmptyException(String message) {
        super(message);
    }
}
