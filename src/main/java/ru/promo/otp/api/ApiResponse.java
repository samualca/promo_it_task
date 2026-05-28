package ru.promo.otp.api;

public record ApiResponse(boolean success, Object data, String error) {
    public static ApiResponse ok(Object data) {
        return new ApiResponse(true, data, null);
    }

    public static ApiResponse error(String error) {
        return new ApiResponse(false, null, error);
    }
}
