package dev.canhdominich.spring.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class TraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String target = handler instanceof HandlerMethod method ? method.getMethod().getName() : handler.getClass().getSimpleName();
        Trace.record("interceptor: preHandle for " + target);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        Trace.record("interceptor: postHandle, view = " + (modelAndView == null ? "none" : modelAndView.getViewName()));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Trace.record("interceptor: afterCompletion, exception = " + (ex == null ? "none" : ex.getClass().getSimpleName()));
    }
}
