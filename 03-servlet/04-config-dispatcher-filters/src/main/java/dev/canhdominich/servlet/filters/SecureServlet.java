package dev.canhdominich.servlet.filters;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class SecureServlet {

    private SecureServlet() {
    }

    @WebServlet("/secure/data")
    public static class Data extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            EventLog.record("secure servlet runs");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("secret data");
        }
    }

    @WebServlet("/annotated")
    public static class Annotated extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            EventLog.record("annotated servlet runs");
            response.getWriter().println("ok");
        }
    }
}
