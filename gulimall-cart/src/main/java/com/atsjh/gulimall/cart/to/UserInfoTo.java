package com.atsjh.gulimall.cart.to;

import lombok.Data;
import lombok.ToString;

/**
 * @author: sjh
 * @date: 2021/7/8 下午4:12
 * @description:
 */
@ToString
@Data
public class UserInfoTo {

    private Long userId;

    private String userKey;

    private Boolean tempUser = false;
}
