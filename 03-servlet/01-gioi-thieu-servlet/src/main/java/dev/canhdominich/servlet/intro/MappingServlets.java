package dev.canhdominich.servlet.intro;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class MappingServlets {

    private MappingServlets() {
    }

    abstract static class Named extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println(getClass().getSimpleName() + " servletPath=" + request.getServletPath() + " pathInfo=" + request.getPathInfo());
        }
    }

    @WebServlet("/orders")
    public static class ExactServlet extends Named {
    }

    @WebServlet("/orders/*")
    public static class PrefixServlet extends Named {
    }

    @WebServlet("*.do")
    public static class ExtensionServlet extends Named {
    }

    public static class ShadowDefaultServlet extends Named {
    }
}
