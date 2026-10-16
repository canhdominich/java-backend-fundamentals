package dev.canhdominich.servlet.reqresp;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public final class SessionServlets {

    private SessionServlets() {
    }

    @WebServlet("/session/start")
    public static class Start extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            HttpSession session = request.getSession();
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("id=" + session.getId() + " new=" + session.isNew() + " maxInactiveInterval=" + session.getMaxInactiveInterval());
        }
    }

    @WebServlet("/session/login")
    public static class Login extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            HttpSession session = request.getSession();
            String before = session.getId();
            if (request.getParameter("rotate") != null) {
                request.changeSessionId();
            }
            session.setAttribute("user", request.getParameter("user"));
            if (request.getParameter("timeout") != null) {
                session.setMaxInactiveInterval(Integer.parseInt(request.getParameter("timeout")));
            }
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("idBefore=" + before + " idAfter=" + request.getSession().getId());
        }
    }

    @WebServlet("/session/whoami")
    public static class WhoAmI extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            HttpSession session = request.getSession(false);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println(session == null ? "anonymous (no session)" : "user=" + session.getAttribute("user"));
        }
    }

    @WebServlet("/session/logout")
    public static class Logout extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("logged out");
        }
    }
}
