<%@ page contentType="text/plain;charset=UTF-8" session="false" %>
<%!
    public void jspInit() {
        dev.canhdominich.jsp.components.EventLog.record("jspInit() called");
    }

    public void jspDestroy() {
        dev.canhdominich.jsp.components.EventLog.record("jspDestroy() called");
    }
%>
<% dev.canhdominich.jsp.components.EventLog.record("_jspService() called on " + Thread.currentThread().getName().replaceAll("\\d+$", "N")); %>
lifecycle page served
