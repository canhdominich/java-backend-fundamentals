package dev.canhdominich.intro.http;

import java.io.File;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

public class ServletServer implements AutoCloseable {

    private final Tomcat tomcat = new Tomcat();

    public ServletServer() throws LifecycleException {
        tomcat.setBaseDir(new File(System.getProperty("java.io.tmpdir"), "intro-tomcat-" + System.nanoTime()).getAbsolutePath());
        tomcat.setPort(0);
        tomcat.getConnector();
        Context context = tomcat.addContext("", new File(System.getProperty("java.io.tmpdir")).getAbsolutePath());
        Tomcat.addServlet(context, "hello", new Servlets.HelloServlet());
        context.addServletMappingDecoded("/hello", "hello");
        Tomcat.addServlet(context, "slow", new Servlets.SlowServlet());
        context.addServletMappingDecoded("/slow", "slow");
        Tomcat.addServlet(context, "agent", new Servlets.AgentServlet());
        context.addServletMappingDecoded("/agent", "agent");
        tomcat.start();
    }

    public int port() {
        return tomcat.getConnector().getLocalPort();
    }

    @Override
    public void close() throws LifecycleException {
        tomcat.stop();
        tomcat.destroy();
    }
}
