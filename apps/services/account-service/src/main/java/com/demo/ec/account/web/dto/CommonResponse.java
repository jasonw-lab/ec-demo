package com.demo.ec.account.web.dto;

public class CommonResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public CommonResponse() {}

    public CommonResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResponse<T> ok(T data) {
        return new CommonResponse<>(true, "OK", data);
    }

    public static <T> CommonResponse<T> fail(String message) {
        return new CommonResponse<>(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
