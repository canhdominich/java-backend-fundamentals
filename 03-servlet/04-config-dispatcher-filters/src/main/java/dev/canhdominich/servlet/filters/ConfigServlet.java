package dev.canhdominich.servlet.filters;

import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/config", initParams = {@WebInitParam(name = "greeting", value = "Xin chào")})
public class ConfigServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getParameter("session") != null) {
            request.getSession();
        }
        if (request.getParameter("logout") != null && request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("ServletConfig  greeting      = " + getServletConfig().getInitParameter("greeting"));
        out.println("ServletConfig  appName       = " + getServletConfig().getInitParameter("appName"));
        out.println("ServletContext appName       = " + getServletContext().getInitParameter("appName"));
        out.println("ServletContext greeting      = " + getServletContext().getInitParameter("greeting"));
        out.println("ServletContext startedBy     = " + getServletContext().getAttribute("startedBy"));
        out.println("ServletContext contextPath   = '" + getServletContext().getContextPath() + "'");
        out.println("ServletContext serverInfo    = " + getServletContext().getServerInfo());
    }
}
