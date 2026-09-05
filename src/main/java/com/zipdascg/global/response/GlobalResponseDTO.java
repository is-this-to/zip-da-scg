package com.zipdascg.global.response;

import com.zipdascg.global.response.constant.CustomResponseCode;

public record GlobalResponseDTO<T>(
        String code
        , String message
        , T data
        , String traceId
) {
    public static <T> GlobalResponseDTO<T> from(
            CustomResponseCode customResponseCode,
            T data,
            String traceId
    ) {
        return new GlobalResponseDTO<>(
                customResponseCode.getCode(),
                customResponseCode.name(),
                data,
                traceId
        );
    }

    public static GlobalResponseDTO<Void> from(
            CustomResponseCode customResponseCode,
            String traceId
    ) {
        return new GlobalResponseDTO<>(
                customResponseCode.getCode(),
                customResponseCode.name(),
                null,
                traceId
        );
    }

    public static <T> GlobalResponseDTO<T> success(
            T data,
            String traceId
    ) {
        return GlobalResponseDTO.from(
                CustomResponseCode.SUCCESS,
                data,
                traceId
        );
    }

    public static GlobalResponseDTO<Void> success(String traceId) {
        return GlobalResponseDTO.from(
                CustomResponseCode.SUCCESS,
                traceId
        );
    }
}
