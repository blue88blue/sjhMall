package com.atsjh.gulimall.thirdparty;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.File;

@SpringBootTest
class GulimallThirdPartyApplicationTests {
    @Resource
    OSSClient ossClient;

    @Test
    void contextLoads() {
        ossClient.putObject("sjhmall", "a.jpg", new File("/home/sjh/dataset/xdata/0c666c10a6684738b7f04ebf52cf4de9.jpeg"));
    }

}
