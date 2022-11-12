package com.rpcpostman;

import com.rpcpostman.dto.WebApiRspDto;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author yudong
 * @date 2022/11/12
 */
@ControllerAdvice
public class GlobalHandler {
    private final Logger logger = Logger.getLogger(getClass());

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public WebApiRspDto<String> exceptionHandler(Exception e) {
        logger.error("error", e);
        return WebApiRspDto.error(ExceptionUtils.getMessage(e) + "\r\n" + ExceptionUtils.getStackTrace(e));
    }

}
