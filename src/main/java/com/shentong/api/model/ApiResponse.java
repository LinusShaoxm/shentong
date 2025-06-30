package com.shentong.api.model;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private String causedBy;
}