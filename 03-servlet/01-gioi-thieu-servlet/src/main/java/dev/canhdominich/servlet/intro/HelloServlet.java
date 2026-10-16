package dev.canhdominich.servlet.intro;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String name = request.getParameter("name");
        String who = name == null || name.isBlank() ? "Servlet" : name;
        response.getWriter().println("<h1>Xin chào từ " + who.replace("<", "&lt;") + "!</h1>");
    }
}
