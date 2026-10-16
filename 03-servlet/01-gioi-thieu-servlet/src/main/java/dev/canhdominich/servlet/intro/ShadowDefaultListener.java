package dev.canhdominich.servlet.intro;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ShadowDefaultListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        if (System.getProperty("lab.shadowDefault") != null) {
            event.getServletContext().addServlet("shadowDefault", new MappingServlets.ShadowDefaultServlet()).addMapping("/");
        }
    }
}
