package com.atsjh.gulimall.cart.interceptor;

import com.atsjh.common.constant.AuthServerConstant;
import com.atsjh.common.constant.CartConstant;
import com.atsjh.common.to.MemberResponseTo;
import com.atsjh.gulimall.cart.to.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * @author: sjh
 * @date: 2021/7/8 下午4:17
 * @description:
 */
public class CartInteceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> toThreadLocal = new ThreadLocal<>();

    /**
     * 目标方法执行之前
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();
        HttpSession session = request.getSession();
        MemberResponseTo loginUser = (MemberResponseTo)session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(loginUser != null){ //用户登录了， 把id赋值
            userInfoTo.setUserId(loginUser.getId());
        }

        //每次访问都要带上一个user_key的Cookie，保存到userInfoTo
        Cookie[] cookies = request.getCookies();
        for(Cookie c : cookies){
            if(CartConstant.TEMP_USER_COOKIE_NAME.equals(c.getName())){
                userInfoTo.setUserKey(c.getValue());
                userInfoTo.setTempUser(true);
            }
        }
        //如果是第一次访问，没有cookie， 就新建一个随机的cookie值
        if(StringUtils.isEmpty(userInfoTo.getUserKey())){
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }

        //将用户信息保存至线程的共享变量中
        toThreadLocal.set(userInfoTo);
        return true;
    }


    /**
     *
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = toThreadLocal.get();
        //如果是第一次访问就让浏览器保存一个cookie
        if(!userInfoTo.getTempUser()){
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
