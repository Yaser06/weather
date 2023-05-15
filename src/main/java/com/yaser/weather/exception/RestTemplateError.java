package com.yaser.weather.exception;

public record RestTemplateError(
        String timestamp,
        String status,
        String error,
        String path
) {
}
