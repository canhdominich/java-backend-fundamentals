package dev.canhdominich.servlet.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

public class ForwardLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        EventLog.record("forwardLogging filter: dispatcher type " + request.getDispatcherType());
        chain.doFilter(request, response);
    }
}
