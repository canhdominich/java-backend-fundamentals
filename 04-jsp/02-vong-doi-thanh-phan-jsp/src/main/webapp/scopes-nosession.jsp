<%@ page contentType="text/plain;charset=UTF-8" session="false" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:if test="${param.page == 'yes'}"><c:set var="who" value="page" scope="page"/></c:if>
${who}|${pageScope.who}|${requestScope.who}|${sessionScope.who}|${applicationScope.who}
