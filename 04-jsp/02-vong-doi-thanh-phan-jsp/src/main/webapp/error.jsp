<%@ page contentType="text/plain;charset=UTF-8" session="false" isErrorPage="true" %>
custom error page
exception class : <%= exception.getClass().getName() %>
exception message: <%= exception.getMessage() %>
response status  : <%= response.getStatus() %>
