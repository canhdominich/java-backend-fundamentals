package dev.canhdominich.servlet.lifecycle;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class LifecycleServlets {

    private LifecycleServlets() {
    }

    abstract static class Recording extends HttpServlet {

        @Override
        public void init() {
            EventLog.record(getClass().getSimpleName() + ".init() on " + Thread.currentThread().getName().replaceAll("\\d+$", "N"));
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            EventLog.record(getClass().getSimpleName() + ".doGet()");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println(getClass().getSimpleName() + " served");
        }

        @Override
        public void destroy() {
            EventLog.record(getClass().getSimpleName() + ".destroy()");
        }
    }

    @WebServlet(urlPatterns = "/life/eager", loadOnStartup = 1)
    public static class EagerServlet extends Recording {
    }

    @WebServlet(urlPatterns = "/life/lazy")
    public static class LazyServlet extends Recording {
    }
}
