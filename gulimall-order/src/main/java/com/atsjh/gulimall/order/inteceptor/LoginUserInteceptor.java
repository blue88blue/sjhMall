package com.atsjh.gulimall.order.inteceptor;

import com.atsjh.common.constant.AuthServerConstant;
import com.atsjh.common.to.MemberResponseTo;
import org.bouncycastle.pqc.jcajce.provider.qtesla.SignatureSpi;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author: sjh
 * @date: 2021/7/10 下午5:00
 * @description:
 */
@Component
public class LoginUserInteceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseTo> toThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/order/order/infoByOrderSn/**", requestURI);
        if(match){
            return true;
        }

        HttpSession session = request.getSession();
        MemberResponseTo user = (MemberResponseTo)session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(user == null){
            session.setAttribute("msg", "!请先登录!");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
        else{
            toThreadLocal.set(user);
            return true;
        }
    }
}
