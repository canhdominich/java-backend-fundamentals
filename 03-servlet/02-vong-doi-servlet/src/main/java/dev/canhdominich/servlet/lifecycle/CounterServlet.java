package dev.canhdominich.servlet.lifecycle;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet({"/count/unsafe", "/count/atomic", "/count/synchronized"})
public class CounterServlet extends HttpServlet {

    private int unsafeCount;
    private int synchronizedCount;
    private final AtomicInteger atomicCount = new AtomicInteger();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String kind = request.getServletPath().substring("/count/".length());
        boolean peek = request.getParameter("peek") != null;
        int value = switch (kind) {
            case "unsafe" -> peek ? unsafeCount : incrementUnsafe();
            case "atomic" -> peek ? atomicCount.get() : atomicCount.incrementAndGet();
            default -> peek ? synchronizedCount : incrementSynchronized();
        };
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().print(value);
    }

    private int incrementUnsafe() {
        int current = unsafeCount;
        Thread.yield();
        unsafeCount = current + 1;
        return unsafeCount;
    }

    private synchronized int incrementSynchronized() {
        int current = synchronizedCount;
        Thread.yield();
        synchronizedCount = current + 1;
        return synchronizedCount;
    }
}
