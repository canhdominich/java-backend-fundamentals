# Bài 01 - Giới thiệu và kiến trúc JSP

Mã nguồn đi kèm bài học **Giới thiệu và kiến trúc JSP** (chương 4: JSP) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một trang JSP đầu tiên, mã servlet mà Jasper sinh ra từ nó, chi phí biên dịch ở lần yêu cầu đầu, khai báo `<%! %>` trở thành trường dùng chung (và mất cập nhật như bài Vòng đời Servlet), trang JSP tự tạo session, ba cách in một tham số (hai cách làm lộ XSS), sửa trang khi server đang chạy, và trang lỗi biên dịch làm lộ mã nguồn.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Apache Tomcat Embed (Jasper) | 11.0.27 | khai báo trong `pom.xml`, gồm cả Jakarta Pages |
| Jakarta Standard Tag Library | 3.0 (API 3.0.2, bản cài đặt 3.0.1) | khai báo trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-gioi-thieu-jsp/
  pom.xml
  src/main/webapp/WEB-INF/web.xml       deployment descriptor tối thiểu (Servlet 6.1)
  src/main/webapp/
    hello.jsp                 trang đầu tiên: HTML, EL, vòng lặp scriptlet
    counter.jsp               khai báo <%! %> làm trường của servlet được sinh ra
    session-default.jsp       trang tự tạo session
    session-off.jsp           trang tắt session bằng session="false"
    xss.jsp                   in một tham số bằng scriptlet, EL và c:out
    broken.jsp                trang có lỗi cú pháp Java
  src/main/java/dev/canhdominich/jsp/intro/
    EmbeddedServer.java       dựng Tomcat nhúng, quét lớp trong target/classes
    ServerMain.java           chạy server ở cổng 8080 (đổi bằng biến PORT)
    JspProbes.java            chạy toàn bộ thí nghiệm và in kết quả
  src/test/java/dev/canhdominich/jsp/intro/
    JspBehaviourTest.java
```

## Chạy server

```bash
mvn -q compile exec:exec
```

Server lắng nghe ở `http://localhost:8080`; mở `http://localhost:8080/hello.jsp`. Dừng bằng Ctrl+C.

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jsp.intro.JspProbes
```

Chương trình mất khoảng 10 giây vì có một bước chờ để Jasper nhận ra trang đã sửa. Các dòng log của Tomcat có ngày giờ (và `SEVERE` khi trang không biên dịch được) là bình thường.

## Kết quả mong đợi

```text
-- hello.jsp, first request compiles the page
first request <N> ms, second request <M> ms, first slower by at least 5x: true

<html>
<body>
    <h1>Xin chào JSP</h1>
    <p>Tổng 1 + 2 tính bằng EL: 3</p>
    <ul>
    
        <li>dòng 1</li>
    
        <li>dòng 2</li>
    
        <li>dòng 3</li>
    
    </ul>
</body>
</html>
-- the servlet that Jasper generated for hello.jsp
  public final class hello_jsp extends org.apache.jasper.runtime.HttpJspBase
  public void _jspService(final jakarta.servlet.http.HttpServletRequest request, final jakarta.servlet.http.Http...
  out.write("\n");
  out.write("<html>\n");
  out.write("<body>\n");
  out.write("    <h1>Xin chào JSP</h1>\n");
  out.write("    <p>Tổng 1 + 2 tính bằng EL: ");
  out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${1 + 2}", java.la...
  out.write("</p>\n");
  out.write("    <ul>\n");
  out.write("    ");
  for (int i = 1; i <= 3; i++) {
  out.write("\n");
  out.write("        <li>dòng ");
  out.print( i );
  out.write("</li>\n");
  out.write("    ");
  out.write("\n");
  out.write("    </ul>\n");
  out.write("</body>\n");
  out.write("</html>\n");
-- a declaration is a field of the generated servlet: 2000 increments from 16 threads
  final value 946
-- does the page create a session?
  default page    Set-Cookie: JSESSIONID=<id>; Path=/; HttpOnly
  session="false" Set-Cookie: none
-- printing the same request parameter three ways


scriptlet : <script>alert(1)</script>
EL        : <script>alert(1)</script>
c:out     : &lt;script&gt;alert(1)&lt;/script&gt;
-- editing a page while the server runs
  before the edit     : version 1
  right after the edit: version 1
  5 seconds later     : version 2
-- a page that does not compile
  status 500, body mentions the source line with secretPrice: true
```

Số mili giây của hai yêu cầu đầu thay đổi theo máy; điều cần thấy là lần đầu chậm hơn nhiều lần. Mã phiên (`JSESSIONID`) thay đổi mỗi lần chạy.

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 6, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Address already in use` ở cổng 8080 | Có ứng dụng khác dùng cổng 8080 | `PORT=9090 mvn -q compile exec:exec` |
| `ClassNotFoundException: org.apache.jasper...` khi mở trang JSP | Thiếu `tomcat-embed-jasper` | Giữ nguyên dependency trong `pom.xml` |
| Sửa JSP mà trang chưa đổi | Jasper chỉ kiểm tra thay đổi theo chu kỳ (khoảng 4 giây) | Đợi vài giây rồi tải lại |
