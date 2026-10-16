package dev.canhdominich.servlet.filters;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StatusServlet extends HttpServlet {

    private String label;

    public StatusServlet() {
    }

    public StatusServlet(String label) {
        this.label = label;
    }

    @Override
    public void init() {
        if (label == null) {
            label = getServletConfig().getInitParameter("label");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("status servlet registered by " + label + " at " + request.getServletPath());
    }
}
