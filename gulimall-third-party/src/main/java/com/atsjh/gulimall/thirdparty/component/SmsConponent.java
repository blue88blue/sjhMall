package com.atsjh.gulimall.thirdparty.component;

import com.atsjh.gulimall.thirdparty.utils.HttpUtils;
import lombok.Data;
import netscape.security.PrivilegeTable;
import org.apache.catalina.Host;
import org.apache.http.HttpResponse;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: sjh
 * @date: 2021/7/5 下午3:27
 * @description:
 */
@Data
@ConfigurationProperties(prefix = "spring.cloud.alicloud.sms")
@Component
public class SmsConponent {
    private String host;
    private String path;
    private String appcode;
    private String smsSignId;
    private String templateId;

    public void sendSmsCode(String phone, String code){
        System.out.println(templateId);
//        String host = "https://gyytz.market.alicloudapi.com";
//        String path = "/sms/smsSend";
        String method = "POST";
//        String appcode = "d3095e9ec830483b97d7c556707d3606";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", phone);
        querys.put("param", "**code**:"+ code +",**minute**:5");
        querys.put("smsSignId", smsSignId); // "2e65b1bb3d054466b82f0c9d125465e2"
        querys.put("templateId", templateId); //"908e94ccf08b4476ba6c876d13f084ad"
        Map<String, String> bodys = new HashMap<String, String>();

        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
