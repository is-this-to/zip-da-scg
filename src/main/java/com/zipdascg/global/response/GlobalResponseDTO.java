package com.zipdascg.global.response;

import com.zipdascg.global.response.constant.CustomResponseCode;

public record GlobalResponseDTO<T>(
        String code
        , String message
        , T data
) {
    public static <T> GlobalResponseDTO<T> from(CustomResponseCode customResponseCode, T data){
       return new GlobalResponseDTO<T>(customResponseCode.getCode(), customResponseCode.name(), data);
    }
    public static <T> GlobalResponseDTO<T> from(CustomResponseCode customResponseCode){
        return new GlobalResponseDTO<T>(customResponseCode.getCode(), customResponseCode.name(), null);
    }
    public static <T> GlobalResponseDTO<T> success(CustomResponseCode customResponseCode, T data){
        return GlobalResponseDTO.from(CustomResponseCode.SUCCESS, data);
    }
    public static <T> GlobalResponseDTO<T> success(CustomResponseCode customResponseCode){
        return GlobalResponseDTO.from(CustomResponseCode.SUCCESS);
    }
}
