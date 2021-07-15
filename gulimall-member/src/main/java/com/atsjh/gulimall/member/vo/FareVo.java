package com.atsjh.gulimall.member.vo;

import com.atsjh.gulimall.member.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    private MemberReceiveAddressEntity address;

    private BigDecimal fare;
}
