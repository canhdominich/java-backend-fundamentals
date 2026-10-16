package dev.canhdominich.servlet.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String path = ((HttpServletRequest) request).getRequestURI();
        EventLog.record("logging filter: before " + path);
        try {
            chain.doFilter(request, response);
        } finally {
            EventLog.record("logging filter: after " + path + ", status " + ((HttpServletResponse) response).getStatus());
        }
    }
}
