<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!doctype html>
<html>
<body>
  <h1>Đăng nhập</h1>
  <c:if test="${not empty error}"><p class="error"><c:out value="${error}"/></p></c:if>
  <form method="post" action="${pageContext.request.contextPath}/login">
    <input type="hidden" name="csrf" value="<c:out value="${csrf}"/>">
    <input type="hidden" name="next" value="<c:out value="${next}"/>">
    <label>Tên đăng nhập <input name="username" value="<c:out value="${username}"/>"></label>
    <label>Mật khẩu <input name="password" type="password"></label>
    <button type="submit">Đăng nhập</button>
  </form>
</body>
</html>
