<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<fmt:setLocale value="vi_VN"/>
<table>
<c:forEach var="p" items="${products}" varStatus="row">
  <tr class="${row.index % 2 == 0 ? 'even' : 'odd'}">
    <td>${row.count}</td>
    <td>${p.sku}</td>
    <td><c:out value="${p.name}"/></td>
    <td><fmt:formatNumber value="${p.price}" type="currency"/></td>
    <td><c:choose><c:when test="${empty p.stock}">unknown</c:when><c:when test="${p.stock == 0}">sold out</c:when><c:otherwise>${p.stock} left</c:otherwise></c:choose></td>
  </tr>
</c:forEach>
</table>
missing variable: [${nothing}] property of a missing variable: [${nothing.name}] empty test: ${empty nothing}
<c:catch var="problem">bad property: [${products[0].nothing}]</c:catch>
bad property raised: ${problem.message}
null compared with 0: [${products[1].stock == 0}] null less than 5: [${products[1].stock < 5}] null plus 1: [${products[1].stock + 1}]
