<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<html>
<body>
<form:form modelAttribute="user" method="post" action="/register">
    <form:input path="name"/> <form:errors path="name" cssClass="error"/>
    <form:input path="email"/> <form:errors path="email" cssClass="error"/>
    <form:input path="age"/> <form:errors path="age" cssClass="error"/>
    <button type="submit">Register</button>
</form:form>
</body>
</html>
