package dev.canhdominich.servlet.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;

public final class AnnotatedFilters {

    private AnnotatedFilters() {
    }

    @WebFilter(filterName = "zebra", urlPatterns = "/annotated")
    public static class Zebra implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            EventLog.record("annotated filter zebra");
            chain.doFilter(request, response);
        }
    }

    @WebFilter(filterName = "apple", urlPatterns = "/annotated")
    public static class Apple implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            EventLog.record("annotated filter apple");
            chain.doFilter(request, response);
        }
    }
}
