package dev.canhdominich.servlet.filters;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public final class DispatchServlets {

    private DispatchServlets() {
    }

    @WebServlet("/dispatch/forward")
    public static class ForwardSource extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            request.setAttribute("message", "set before forward");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("this text is discarded by forward");
            request.getRequestDispatcher("/dispatch/target").forward(request, response);
        }
    }

    @WebServlet("/dispatch/include")
    public static class IncludeSource extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            request.setAttribute("message", "set before include");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("--- before include (written by the including servlet)");
            request.getRequestDispatcher("/dispatch/target").include(request, response);
            response.getWriter().println("--- after include (written by the including servlet)");
        }
    }

    @WebServlet("/dispatch/redirect")
    public static class RedirectSource extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            request.setAttribute("message", "set before redirect");
            response.sendRedirect("/dispatch/target");
        }
    }

    @WebServlet("/dispatch/target")
    public static class Target extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            boolean included = request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
            if (!included) {
                response.setStatus(HttpServletResponse.SC_ACCEPTED);
                response.setContentType("text/plain;charset=UTF-8");
            }
            response.setHeader("X-Set-By-Target", "yes");
            PrintWriter out = response.getWriter();
            out.println("target: getRequestURI    = " + request.getRequestURI());
            out.println("target: forward original = " + request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI));
            out.println("target: include uri attr = " + request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI));
            out.println("target: message attribute = " + request.getAttribute("message"));
        }
    }
}
