package dev.canhdominich.spring.mvc;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public final class EmbeddedServer implements AutoCloseable {

    private final Tomcat tomcat;
    private final AnnotationConfigWebApplicationContext springContext;

    private EmbeddedServer(Tomcat tomcat, AnnotationConfigWebApplicationContext springContext) {
        this.tomcat = tomcat;
        this.springContext = springContext;
    }

    public static EmbeddedServer start(int port, Class<?>... configurations) throws LifecycleException {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.WARNING);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(Level.WARNING);
        }
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(new File("target/tomcat").getAbsolutePath());
        tomcat.setPort(port);
        Connector connector = tomcat.getConnector();
        connector.setProperty("address", "127.0.0.1");
        Context context = tomcat.addContext("", new File("src/main/webapp").getAbsolutePath());
        if (context instanceof org.apache.catalina.core.StandardContext standard) {
            standard.setClearReferencesRmiTargets(false);
            standard.setClearReferencesThreadLocals(false);
        }
        context.addLifecycleListener(new Tomcat.FixContextListener());
        context.addServletContainerInitializer(new org.apache.jasper.servlet.JasperInitializer(), null);
        Wrapper jsp = Tomcat.addServlet(context, "jsp", "org.apache.jasper.servlet.JspServlet");
        jsp.setLoadOnStartup(3);
        context.addServletMappingDecoded("*.jsp", "jsp");

        AnnotationConfigWebApplicationContext springContext = new AnnotationConfigWebApplicationContext();
        springContext.register(configurations);
        Wrapper dispatcher = Tomcat.addServlet(context, "dispatcher", new DispatcherServlet(springContext));
        dispatcher.setLoadOnStartup(1);
        context.addServletMappingDecoded("/", "dispatcher");

        tomcat.start();
        return new EmbeddedServer(tomcat, springContext);
    }

    public int port() {
        return tomcat.getConnector().getLocalPort();
    }

    public AnnotationConfigWebApplicationContext springContext() {
        return springContext;
    }

    @Override
    public void close() throws LifecycleException {
        tomcat.stop();
        tomcat.destroy();
    }
}
