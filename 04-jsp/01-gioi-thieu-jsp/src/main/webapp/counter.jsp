<%@ page contentType="text/plain;charset=UTF-8" session="false" %>
<%!
    private int visits;

    private int increment() {
        int current = visits;
        Thread.yield();
        visits = current + 1;
        return visits;
    }
%>
<% if (request.getParameter("peek") != null) { out.print(visits); } else { out.print(increment()); } %>
