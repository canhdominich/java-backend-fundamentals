<%@ page contentType="text/plain;charset=UTF-8" %>
session id in this page: <%= session.getId().length() > 0 ? "created" : "none" %>
