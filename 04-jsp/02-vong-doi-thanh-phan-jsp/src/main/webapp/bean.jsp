<%@ page contentType="text/plain;charset=UTF-8" %>
<jsp:useBean id="cart" class="dev.canhdominich.jsp.components.Cart" scope="session"/>
<jsp:setProperty name="cart" property="items" value="${cart.items + 1}"/>
owner=<jsp:getProperty name="cart" property="owner"/> items=<jsp:getProperty name="cart" property="items"/>
