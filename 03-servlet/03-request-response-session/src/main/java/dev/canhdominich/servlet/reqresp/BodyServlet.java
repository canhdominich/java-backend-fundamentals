package dev.canhdominich.servlet.reqresp;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

public final class BodyServlet {

    private BodyServlet() {
    }

    @WebServlet("/body/read-first")
    public static class ReadFirst extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String body = request.getReader().lines().collect(Collectors.joining("\n"));
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("body length = " + body.length());
            response.getWriter().println("ten after reading the body = " + request.getParameter("ten"));
        }
    }

    @WebServlet("/body/parameter-first")
    public static class ParameterFirst extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String ten = request.getParameter("ten");
            String body = request.getReader().lines().collect(Collectors.joining("\n"));
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("ten = " + ten);
            response.getWriter().println("body length after getParameter = " + body.length());
        }
    }

    @WebServlet("/body/json")
    public static class Json extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String body = request.getReader().lines().collect(Collectors.joining("\n"));
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"received\":" + body.length() + ",\"contentType\":\"" + request.getContentType() + "\"}");
        }
    }
}
