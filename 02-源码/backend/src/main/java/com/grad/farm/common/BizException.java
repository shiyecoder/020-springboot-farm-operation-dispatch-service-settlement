package com.grad.farm.common;

import lombok.Getter;

/** 业务异常，交给全局处理器转成统一响应体。 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) { this(400, message); }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
