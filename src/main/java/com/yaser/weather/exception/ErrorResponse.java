package com.yaser.weather.exception;

public record ErrorResponse(
        String success,
        Error error
) {
}
