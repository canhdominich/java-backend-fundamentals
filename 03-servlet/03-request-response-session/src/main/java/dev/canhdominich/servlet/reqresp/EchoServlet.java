package dev.canhdominich.servlet.reqresp;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

@WebServlet("/echo")
public class EchoServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("method        = " + request.getMethod());
        out.println("requestURI    = " + request.getRequestURI());
        out.println("contextPath   = '" + request.getContextPath() + "'");
        out.println("servletPath   = " + request.getServletPath());
        out.println("queryString   = " + request.getQueryString());
        out.println("name          = " + request.getParameter("name"));
        out.println("hobby         = " + Arrays.toString(request.getParameterValues("hobby")));
        out.println("missing       = " + request.getParameter("missing"));
        out.println("User-Agent    = " + request.getHeader("user-agent"));
        out.println("contentType   = " + request.getContentType());
        out.println("remoteAddr    = " + request.getRemoteAddr());
    }
}
