# Bài 01 - Spring Security: kiến trúc và cấu hình mặc định

Mã nguồn đi kèm bài học **Giới thiệu và kiến trúc Spring Security** (chương 11: Spring Security) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: chỉ thêm `spring-boot-starter-security` và xem điều gì thay đổi: mật khẩu sinh ngẫu nhiên in ra lúc khởi động, chuỗi 16 bộ lọc (`SecurityFilterChain`) theo đúng thứ tự, câu trả lời khác nhau cho client chỉ muốn JSON (401) và trình duyệt (302 tới `/login`), bảo mật chạy trước định tuyến (đường dẫn lạ: 401 với người lạ, 404 với người đã đăng nhập), các header an toàn mặc định, bảo vệ CSRF và vì sao POST với Basic nhận 401 còn POST với session nhận 403, và đăng nhập bằng form với phiên đổi mã sau khi đăng nhập.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml`; quản lý Spring Security 7.1.1 |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-kien-truc-mac-dinh/
  pom.xml                       parent Boot 4.1.1, starter webmvc, security, test
  src/main/java/dev/canhdominich/boot/security/
    SecurityApplication.java    @SpringBootApplication, không có cấu hình bảo mật nào
    HelloController.java        GET /hello trả tên người dùng, POST /orders
    DefaultSecurityDemo.java    khởi động ứng dụng, gửi yêu cầu thật và in kết quả
  src/test/java/dev/canhdominich/boot/security/
    DefaultSecurityTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `DefaultSecurityDemo` (đã bỏ log ở stderr; mật khẩu sinh ngẫu nhiên đổi mỗi lần chạy nên chỉ in độ dài):

```text
generated password logged at startup: true (a random UUID, 36 characters)
filters in the default chain: [DisableEncodeUrlFilter, WebAsyncManagerIntegrationFilter, SecurityContextHolderFilter, HeaderWriterFilter, CsrfFilter, LogoutFilter, UsernamePasswordAuthenticationFilter, DefaultResourcesFilter, DefaultLoginPageGeneratingFilter, DefaultLogoutPageGeneratingFilter, BasicAuthenticationFilter, RequestCacheAwareFilter, SecurityContextHolderAwareRequestFilter, AnonymousAuthenticationFilter, ExceptionTranslationFilter, AuthorizationFilter]
-- who gets in
GET /hello (no credentials) -> 401, WWW-Authenticate: Basic realm="Realm", charset="UTF-8"
GET /hello (no credentials) Accept text/html -> 302, Location: /login
GET /hello (wrong password) -> 401, WWW-Authenticate: Basic realm="Realm", charset="UTF-8"
GET /hello (right password) -> 200, body {"message":"hello user"}
GET /no-such-page (no credentials) -> 401, WWW-Authenticate: Basic realm="Realm", charset="UTF-8"
GET /no-such-page (right password) -> 404
-- headers added to an authenticated answer
response headers: [cache-control, content-type, expires, pragma, x-content-type-options, x-frame-options, x-xss-protection]
-- CSRF protection on POST
POST /orders (right password) -> 401, WWW-Authenticate: Basic realm="Realm", charset="UTF-8"
-- the default login page and form login
GET /login -> 200, form has username, password and _csrf fields: true, session cookie attributes: [Path=/, HttpOnly]
POST /login with a wrong password -> 302, Location /login?error
POST /login with the right password and the token -> 302, Location /, session id changed: true
GET /hello with only the session cookie -> 200 {"message":"hello user"}
POST /orders with the session cookie and no CSRF token -> 403
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 5, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Không tìm thấy mật khẩu sinh ngẫu nhiên trong log | Mức log gốc đang tắt | Đặt `logging.level.root=INFO` như `DefaultSecurityDemo` |
| Dòng `POST /orders` cho 403 thay vì 401 | Client có phiên đã đăng nhập chứ không phải Basic | Đúng như bài: 403 cho người đã xác thực, 401 cho người chưa xác thực |
