package com.master.excel.parser.exception;

import com.master.excel.parser.dto.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidExtension.class)
    public ResponseEntity<?> handleInvalidExtension(InvalidExtension e){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST.value())
                .body(new ExceptionResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), e.getMessage(), "/api/upload"));
    }

    @ExceptionHandler(VehicleTypeUndefined.class)
    public ResponseEntity<?> handleVehicleTypeUndefined(VehicleTypeUndefined e){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ExceptionResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), e.getMessage(), "/api/upload"));
    }

    @ExceptionHandler(FileConversionException.class)
    public ResponseEntity<?> handleFileConversionException(FileConversionException e){
        return ResponseEntity
                .status(HttpStatus.PRECONDITION_FAILED.value())
                .body(new ExceptionResponse(LocalDateTime.now(), HttpStatus.PRECONDITION_FAILED.value(), e.getMessage(), "/api/upload"));
    }

    @ExceptionHandler(CubicCapacityException.class)
    public ResponseEntity<?> handleCubicCapacityException(CubicCapacityException e){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST.value())
                .body(new ExceptionResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), e.getMessage(), "/api/upload"));
    }

}
