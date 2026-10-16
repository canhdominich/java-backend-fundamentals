package dev.canhdominich.servlet.reqresp;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class ResponseServlets {

    private ResponseServlets() {
    }

    @WebServlet("/resp/status")
    public static class Status extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setHeader("Location", request.getContextPath() + "/orders/42");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("created");
        }
    }

    @WebServlet("/resp/error")
    public static class Error extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "no such order");
        }
    }

    @WebServlet("/resp/redirect")
    public static class Redirect extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.sendRedirect(request.getContextPath() + "/echo?name=after-redirect");
        }
    }

    @WebServlet("/resp/committed")
    public static class Committed extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("bufferSize=" + response.getBufferSize() + " committedBefore=" + response.isCommitted());
            response.flushBuffer();
            String outcome;
            try {
                response.sendRedirect("/echo");
                outcome = "redirect accepted";
            } catch (IllegalStateException e) {
                outcome = "IllegalStateException: " + e.getMessage();
            }
            response.getWriter().println("committedAfter=" + response.isCommitted() + " " + outcome);
        }
    }

    @WebServlet("/resp/cookie")
    public static class CookieServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            Cookie theme = new Cookie("theme", "dark");
            theme.setMaxAge(7 * 24 * 3600);
            theme.setPath("/");
            theme.setHttpOnly(true);
            theme.setSecure(true);
            theme.setAttribute("SameSite", "Lax");
            response.addCookie(theme);
            response.setContentType("text/plain;charset=UTF-8");
            StringBuilder seen = new StringBuilder();
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    seen.append(cookie.getName()).append('=').append(cookie.getValue()).append(' ');
                }
            }
            response.getWriter().println("cookies received: " + seen.toString().trim());
        }
    }
}
