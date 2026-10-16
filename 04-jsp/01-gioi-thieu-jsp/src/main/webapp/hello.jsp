<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<body>
    <h1>Xin chào JSP</h1>
    <p>Tổng 1 + 2 tính bằng EL: ${1 + 2}</p>
    <ul>
    <% for (int i = 1; i <= 3; i++) { %>
        <li>dòng <%= i %></li>
    <% } %>
    </ul>
</body>
</html>
