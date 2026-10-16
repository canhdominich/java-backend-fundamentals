package dev.canhdominich.servlet.reqresp;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

public final class EmbeddedServer implements AutoCloseable {

    private final Tomcat tomcat;

    private EmbeddedServer(Tomcat tomcat) {
        this.tomcat = tomcat;
    }

    public static EmbeddedServer start(int port) throws LifecycleException {
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
        if (System.getProperty("lab.hardenErrors") != null) {
            org.apache.catalina.valves.ErrorReportValve valve = new org.apache.catalina.valves.ErrorReportValve();
            valve.setShowReport(false);
            valve.setShowServerInfo(false);
            tomcat.getHost().getPipeline().addValve(valve);
        }
        Context context = tomcat.addWebapp("", new File("src/main/webapp").getAbsolutePath());
        if (context instanceof org.apache.catalina.core.StandardContext standard) {
            standard.setClearReferencesRmiTargets(false);
            standard.setClearReferencesThreadLocals(false);
        }
        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", new File("target/classes").getAbsolutePath(), "/"));
        context.setResources(resources);
        tomcat.start();
        return new EmbeddedServer(tomcat);
    }

    public int port() {
        return tomcat.getConnector().getLocalPort();
    }

    public Tomcat tomcat() {
        return tomcat;
    }

    @Override
    public void close() throws LifecycleException {
        tomcat.stop();
        tomcat.destroy();
    }
}
