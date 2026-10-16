package dev.canhdominich.servlet.intro;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet(urlPatterns = "/who", loadOnStartup = 1)
public class InstanceServlet extends HttpServlet {

    private static final AtomicInteger INSTANCES = new AtomicInteger();
    private final AtomicInteger requests = new AtomicInteger();

    @Override
    public void init() {
        INSTANCES.incrementAndGet();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        response.getWriter().println(System.identityHashCode(this) + "," + Thread.currentThread().getName() + "," + requests.incrementAndGet() + "," + INSTANCES.get());
    }
}
