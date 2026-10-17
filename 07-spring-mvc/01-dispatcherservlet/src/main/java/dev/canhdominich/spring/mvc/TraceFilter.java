package dev.canhdominich.spring.mvc;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

public class TraceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Trace.record("filter: before the servlet");
        try {
            chain.doFilter(request, response);
        } finally {
            Trace.record("filter: after the servlet");
        }
    }
}
