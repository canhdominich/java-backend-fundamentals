# Bài 03 - So sánh Servlet và JSP, ứng dụng MVC đăng nhập

Mã nguồn đi kèm bài học **So sánh Servlet và JSP** (chương 4: JSP) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy một ứng dụng web nhỏ theo mô hình MVC: servlet làm controller, JSP trong `WEB-INF` làm view, JDBC với H2 trong bộ nhớ làm model. Ứng dụng có đăng nhập và đăng xuất với mã chống CSRF, đổi mã session khi đăng nhập, mẫu Post/Redirect/Get, băm mật khẩu bằng PBKDF2 có muối, và chặn dò tên người dùng qua thời gian phản hồi.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Apache Tomcat Embed (Jasper) | 11.0.27 | khai báo trong `pom.xml` |
| Jakarta Standard Tag Library | 3.0 (API 3.0.2, bản cài đặt 3.0.1) | khai báo trong `pom.xml` |
| H2 Database | 2.5.252 | cơ sở dữ liệu trong bộ nhớ, khai báo trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
03-servlet-va-jsp-mvc/
  pom.xml
  src/main/webapp/WEB-INF/web.xml       deployment descriptor tối thiểu (Servlet 6.1)
  src/main/webapp/WEB-INF/views/
    login.jsp                 form đăng nhập, in dữ liệu bằng c:out
    dashboard.jsp             trang sau đăng nhập
  src/main/java/dev/canhdominich/jsp/mvc/
    EmbeddedServer.java       dựng Tomcat nhúng, quét lớp trong target/classes
    ServerMain.java           chạy server ở cổng 8080 (đổi bằng biến PORT)
    PasswordHasher.java       PBKDF2WithHmacSHA256 có muối, 600000 vòng lặp
    UserDao.java              H2 trong bộ nhớ, PreparedStatement, hai cách xác thực
    EncodingFilter.java       đặt UTF-8 cho mọi yêu cầu
    AuthenticationFilter.java chuyển hướng người chưa đăng nhập khỏi /dashboard
    LoginServlet.java         hiển thị form, kiểm tra CSRF và mật khẩu, đổi mã session
    DashboardServlet.java     forward sang dashboard.jsp
    LogoutServlet.java        hủy session rồi chuyển hướng
    MvcProbes.java            chạy toàn bộ luồng và đo thời gian
  src/test/java/dev/canhdominich/jsp/mvc/
    MvcBehaviourTest.java
```

## Chạy server

```bash
mvn -q compile exec:exec
```

Server lắng nghe ở `http://localhost:8080`; mở `http://localhost:8080/login` và đăng nhập bằng `an` và `secret123`. Dừng bằng Ctrl+C.

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jsp.mvc.MvcProbes
```

Chương trình mất khoảng 20 giây, phần lớn là các lần băm mật khẩu để đo thời gian. Tài khoản mẫu: `an` với mật khẩu `secret123`, và `binh` với `another-secret`.

## Kết quả mong đợi

```text
-- the flow of one browser
GET /dashboard without login      -> 302 Location=/login?next=%2Fdashboard
GET /login                        -> 200, form has a CSRF token of 32 characters
POST /login without token         -> 403
POST /login with a wrong password -> 401, message shown: true, username echoed escaped: true, raw tag echoed: false
POST /login with the right one    -> 302 Location=/dashboard
session id changed by the login   : true
GET /dashboard after login        -> 200, greets: true
GET /WEB-INF/views/dashboard.jsp  -> 404
POST /logout                      -> 302 Location=/login
GET /dashboard after logout       -> 302
-- password hashing cost and user enumeration by timing
stored hash format: 600000:<salt>:<hash>
one PBKDF2 hash with 600000 iterations: 716 ms
authenticate, wrong password: 723 ms, unknown user: 719 ms
naive version, wrong password: 730 ms, unknown user: 0.9 ms
```

Các con số mili giây thay đổi theo máy và JDK; điều cần thấy là băm mật khẩu tốn hàng trăm mili giây, và phiên bản ngây thơ trả lời người dùng không tồn tại nhanh hơn gấp nhiều lần so với sai mật khẩu.

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 8, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Address already in use` ở cổng 8080 | Có ứng dụng khác dùng cổng 8080 | `PORT=9090 mvn -q compile exec:exec` |
