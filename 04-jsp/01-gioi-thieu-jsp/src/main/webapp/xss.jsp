<%@ page contentType="text/plain;charset=UTF-8" session="false" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
scriptlet : <%= request.getParameter("q") %>
EL        : ${param.q}
c:out     : <c:out value="${param.q}"/>
