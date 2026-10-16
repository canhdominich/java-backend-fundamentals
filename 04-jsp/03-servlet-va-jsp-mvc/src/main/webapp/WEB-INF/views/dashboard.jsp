<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!doctype html>
<html>
<body>
  <h1>Xin chào, <c:out value="${sessionScope.displayName}"/></h1>
  <p>Bạn đang đăng nhập với tài khoản <c:out value="${sessionScope.user}"/>.</p>
  <form method="post" action="${pageContext.request.contextPath}/logout"><button type="submit">Đăng xuất</button></form>
</body>
</html>
