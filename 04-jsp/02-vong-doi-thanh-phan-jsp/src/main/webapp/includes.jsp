<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ include file="header.jspf" %>
<main>
  static include shares variables: <%= sharedTitle %>
  <jsp:include page="header-dynamic.jsp">
    <jsp:param name="title" value="from the including page"/>
  </jsp:include>
</main>
