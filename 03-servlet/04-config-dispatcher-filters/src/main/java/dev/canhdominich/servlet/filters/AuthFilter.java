package dev.canhdominich.servlet.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthFilter implements Filter {

    private String expectedToken;

    @Override
    public void init(FilterConfig config) {
        expectedToken = config.getInitParameter("token");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = ((HttpServletRequest) request).getHeader("X-Token");
        if (!expectedToken.equals(token)) {
            EventLog.record("auth filter: rejected, chain not continued");
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "token required");
            return;
        }
        EventLog.record("auth filter: token accepted");
        chain.doFilter(request, response);
    }
}
