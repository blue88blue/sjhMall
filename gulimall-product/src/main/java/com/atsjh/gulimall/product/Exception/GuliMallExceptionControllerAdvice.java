package com.atsjh.gulimall.product.Exception;

import com.atsjh.common.exception.BizCodeEnum;
import com.atsjh.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: sjh
 * @date: 2021/6/13 下午8:52
 * @description:
 */
@Slf4j
@ResponseBody
@RestControllerAdvice("com.atsjh.gulimall.product.controller")
public class GuliMallExceptionControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handelVaildException(MethodArgumentNotValidException e){
        log.error("数据校验问题:{}", e.getMessage());

        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> map = new HashMap<>();

        bindingResult.getFieldErrors().forEach((item)->{
            String defaultMessage = item.getDefaultMessage();
            String field = item.getField();
            map.put(field, defaultMessage);
        });

        return R.error(BizCodeEnum.VALID_EXCEPTION.getCode(), BizCodeEnum.VALID_EXCEPTION.getMsg()).put("data", map);
    }

    @ExceptionHandler(Throwable.class)
    public R handleException(){
        return R.error(BizCodeEnum.UNKNOW_EXEPTION.getCode(), BizCodeEnum.UNKNOW_EXEPTION.getMsg());
    }
}
