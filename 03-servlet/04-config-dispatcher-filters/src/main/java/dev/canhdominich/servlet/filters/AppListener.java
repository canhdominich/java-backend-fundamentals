package dev.canhdominich.servlet.filters;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.concurrent.atomic.AtomicInteger;

@WebListener
public class AppListener implements ServletContextListener, HttpSessionListener {

    private final AtomicInteger activeSessions = new AtomicInteger();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        String appName = event.getServletContext().getInitParameter("appName");
        event.getServletContext().setAttribute("startedBy", "AppListener for " + appName);
        event.getServletContext().setAttribute("activeSessions", activeSessions);
        EventLog.record("context initialized for " + appName);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        EventLog.record("context destroyed");
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        EventLog.record("session created, active=" + activeSessions.incrementAndGet());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        EventLog.record("session destroyed, active=" + activeSessions.decrementAndGet());
    }
}
