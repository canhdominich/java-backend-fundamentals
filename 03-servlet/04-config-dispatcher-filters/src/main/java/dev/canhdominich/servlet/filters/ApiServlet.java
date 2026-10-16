package dev.canhdominich.servlet.filters;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@WebServlet("/api/echo")
public class ApiServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String first = request.getReader().lines().collect(Collectors.joining("\n"));
        String second = request.getReader().lines().collect(Collectors.joining("\n"));
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("servlet read " + first.length() + " characters, a second read gave " + second.length());
        response.getWriter().println("wrapper class seen by the servlet: " + request.getClass().getSimpleName());
    }
}
