package dev.canhdominich.jsp.components;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WebServlet("/scopes")
public class ScopesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String parameter = request.getParameter("set");
        List<String> scopes = parameter == null || parameter.isBlank() ? List.of() : Arrays.asList(parameter.split(","));
        if (scopes.contains("request")) {
            request.setAttribute("who", "request");
        }
        if (scopes.contains("session")) {
            request.getSession().setAttribute("who", "session");
        }
        if (scopes.contains("application")) {
            getServletContext().setAttribute("who", "application");
        }
        request.getRequestDispatcher("/scopes.jsp").forward(request, response);
    }
}
