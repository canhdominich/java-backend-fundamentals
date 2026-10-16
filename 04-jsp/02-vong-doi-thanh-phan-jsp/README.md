# Bài 02 - Vòng đời và thành phần JSP

Mã nguồn đi kèm bài học **Vòng đời và thành phần JSP** (chương 4: JSP) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: `jspInit`, `_jspService` và `jspDestroy` ghi lại theo thời gian thực, include tĩnh so với include động (và loại điều phối `INCLUDE` mà filter thấy), thứ tự EL tìm một tên qua các phạm vi, vì sao `sessionScope` rỗng trong trang `session="false"`, JSTL với `record`, EL khi thiếu biến hoặc thiếu thuộc tính, định dạng tiền tệ theo locale, trang lỗi `errorPage`, và `jsp:useBean`.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Apache Tomcat Embed (Jasper) | 11.0.27 | khai báo trong `pom.xml`, gồm cả Jakarta Pages và Expression Language |
| Jakarta Standard Tag Library | 3.0 (API 3.0.2, bản cài đặt 3.0.1) | khai báo trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-vong-doi-thanh-phan-jsp/
  pom.xml
  src/main/webapp/WEB-INF/web.xml       deployment descriptor tối thiểu (Servlet 6.1)
  src/main/webapp/
    lifecycle.jsp             ghi đè jspInit, jspDestroy và ghi sự kiện ở _jspService
    header.jspf               đoạn trang dùng cho include tĩnh, khai báo một biến
    header-dynamic.jsp        trang dùng cho include động, đọc tham số title
    includes.jsp              include tĩnh rồi include động
    scopes.jsp                in ${who} và bốn phạm vi tường minh
    scopes-nosession.jsp      như trên nhưng session="false"
    products.jsp              JSTL: forEach, choose, formatNumber, c:catch
    throws.jsp, error.jsp     trang ném lỗi và trang lỗi errorPage
    bean.jsp                  jsp:useBean, jsp:setProperty, jsp:getProperty
  src/main/java/dev/canhdominich/jsp/components/
    EmbeddedServer.java       dựng Tomcat nhúng, quét lớp trong target/classes
    ServerMain.java           chạy server ở cổng 8080 (đổi bằng biến PORT)
    EventLog.java             in các sự kiện vòng đời
    Product.java              record dùng cho danh sách sản phẩm
    Cart.java                 JavaBean cho jsp:useBean
    ScopesServlet.java        đặt thuộc tính ở các phạm vi rồi forward sang scopes.jsp
    ProductsServlet.java      đặt danh sách sản phẩm rồi forward sang products.jsp
    IncludeLoggingFilter.java in loại điều phối mà filter thấy cho header-dynamic.jsp
    ComponentProbes.java      chạy toàn bộ thí nghiệm và in kết quả
  src/test/java/dev/canhdominich/jsp/components/
    ComponentBehaviourTest.java
```

## Chạy server

```bash
mvn -q compile exec:exec
```

Server lắng nghe ở `http://localhost:8080`; mở `http://localhost:8080/lifecycle.jsp`, `/includes.jsp`, `/products`. Dừng bằng Ctrl+C.

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jsp.components.ComponentProbes
```

Các dòng `event:` do trang JSP, servlet và filter tự in. Các dòng log của Tomcat có ngày giờ là bình thường.

## Kết quả mong đợi

```text
-- lifecycle of a JSP: two requests, then the server stops
  event: jspInit() called
  event: _jspService() called on http-nio-127.0.0.1-auto-1-exec-N
  event: _jspService() called on http-nio-127.0.0.1-auto-1-exec-N
-- static include and dynamic include
  event: filter sees /includes.jsp with dispatcher type INCLUDE
<header>static header text</header>
<main>
  static include shares variables: defined in the static header
  
<header>dynamic header, param title = from the including page</header>
</main>
generated includes_jsp.java contains the static header text itself : true
generated includes_jsp.java calls the dynamic header at request time: true
-- which scope does ${who} read? (page|requestScope|sessionScope|applicationScope shown after it)
  set=(nothing)                      page=no   -> ||||
  set=application                    page=no   -> application||||application
  set=session,application            page=no   -> session|||session|application
  set=request,session,application    page=no   -> request||request|session|application
  set=request,session,application    page=yes  -> page|page|request|session|application
-- the same attributes read by a page with session="false"
  page with a session : session|||session|application
  page session=false  : application||||application
-- JSTL and EL on a list of records
<table>
  <tr class="even">
    <td>1</td>
    <td>KB-01</td>
    <td>Keyboard</td>
    <td>50 ₫</td>
    <td>12 left</td>
  </tr>
  <tr class="odd">
    <td>2</td>
    <td>MS-02</td>
    <td>Mouse &lt;b&gt;Pro&lt;/b&gt;</td>
    <td>20 ₫</td>
    <td>unknown</td>
  </tr>
  <tr class="even">
    <td>3</td>
    <td>HD-03</td>
    <td>Monitor</td>
    <td>1.299 ₫</td>
    <td>sold out</td>
  </tr>
</table>
missing variable: [] property of a missing variable: [] empty test: true
bad property: [
bad property raised: Property [nothing] not found on type [dev.canhdominich.jsp.components.Product]
-- error page
status 500
custom error page
exception class : java.lang.IllegalStateException
exception message: the report generator failed
response status  : 500
-- jsp:useBean in the session scope
owner=guest items=1
owner=guest items=2
-- stopping the server
  event: jspDestroy() called
```

Dấu cách giữa số và ký hiệu `₫` trong cột giá là ký tự không ngắt dòng (U+00A0) do JDK sinh ra theo locale `vi_VN`; số `49.90` bị làm tròn thành `50` vì tiền đồng không có chữ số thập phân trong locale này.

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 9, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Address already in use` ở cổng 8080 | Có ứng dụng khác dùng cổng 8080 | `PORT=9090 mvn -q compile exec:exec` |
