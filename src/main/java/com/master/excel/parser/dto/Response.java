package com.master.excel.parser.dto;

import java.time.LocalDateTime;

public class Response {

    public Response() {
    }

    public Response(LocalDateTime timestamp, Integer status, String message, String data) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    private LocalDateTime timestamp;
    private Integer status;
    private String message;
    private String data;
}
