package dev.canhdominich.servlet.lifecycle;

import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public final class FailingInitServlet {

    private static final AtomicInteger TEMPORARY_ATTEMPTS = new AtomicInteger();

    private FailingInitServlet() {
    }

    @WebServlet("/bad-init/ordinary")
    public static class Ordinary extends HttpServlet {

        @Override
        public void init() throws ServletException {
            if (TEMPORARY_ATTEMPTS.incrementAndGet() == 1) {
                throw new ServletException("database not ready yet");
            }
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("ready after " + TEMPORARY_ATTEMPTS.get() + " init attempts");
        }
    }

    @WebServlet("/bad-init/unavailable")
    public static class Unavailable extends HttpServlet {

        @Override
        public void init() throws ServletException {
            throw new UnavailableException("configuration missing");
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.getWriter().println("never reached");
        }
    }
}
