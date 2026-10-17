# Bài 03 - JWT: xác thực không trạng thái cho API

Mã nguồn đi kèm bài học **JWT Authentication** (chương 11: Spring Security) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một API đăng nhập bằng tên và mật khẩu để nhận JWT (HS256) rồi gọi các endpoint bằng `Authorization: Bearer`, không có phiên hay cookie; nội dung token đọc được bằng Base64 (header và payload không mã hóa), phân quyền theo claim `roles`, các token bị từ chối (đổi payload giữ chữ ký cũ, `alg: none`, ký bằng khóa khác, rác), thời hạn token với khoảng bù một phút, và việc không có cách thu hồi: token của người dùng đã bị xóa vẫn dùng được tới khi hết hạn.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml`; quản lý Spring Security 7.1.1 |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
03-jwt/
  pom.xml                       parent Boot 4.1.1, starter security, oauth2-resource-server, test
  src/main/java/dev/canhdominich/boot/jwt/
    JwtApplication.java         @SpringBootApplication
    JwtSecurityConfig.java      khóa ký, JwtEncoder, JwtDecoder, người dùng trong bộ nhớ, chuỗi bộ lọc không trạng thái
    TokenService.java           phát hành token với claim sub, iat, exp và roles
    AuthController.java         POST /auth/login, GET /api/me, GET /admin/stats
    JwtDemo.java                chạy mọi thí nghiệm
  src/test/java/dev/canhdominich/boot/jwt/
    JwtBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `JwtDemo` (đã bỏ log ở stderr; thời điểm và cổng được thay bằng `<time>` và `<port>`):

```text
-- logging in
login an -> 200, token received: true
login admin -> 200, token received: true
login with a wrong password -> 401
-- what a token contains (anyone can read it)
parts separated by dots: 3
header: {"alg":"HS256"}
payload claims: [exp, iat, roles, sub], sub an, roles [USER], lifetime 900 s
-- calling the API
no token -> 401, WWW-Authenticate: Bearer resource_metadata="http://127.0.0.1:<port>/.well-known/oauth-protected-resource"
an token on /api/me -> 200, {"user":"an","authorities":["FACTOR_BEARER","ROLE_USER"]}
an token on /admin/stats -> 403, WWW-Authenticate: Bearer error="insufficient_scope", error_description="The request requires higher privileges than provided by the access token.", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"
admin token on /admin/stats -> 200, {"stats":"secret numbers"}
session cookie set by the server: none
-- tokens that must be refused
payload changed to ADMIN, old signature -> 401, WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode the Jwt: Signed JWT rejected: Invalid signature", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1", resource_metadata="http://127.0.0.1:<port>/.well-known/oauth-protected-r...
alg none, no signature -> 401, WWW-Authenticate: Bearer error="invalid_token", error_description="Unsupported algorithm of none", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1", resource_metadata="http://127.0.0.1:<port>/.well-known/oauth-protected-resource"
signed with another secret -> 401, WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode the Jwt: Signed JWT rejected: Invalid signature", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1", resource_metadata="http://127.0.0.1:<port>/.well-known/oauth-protected-r...
garbage -> 401, WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode the Jwt: Malformed token", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1", resource_metadata="http://127.0.0.1:<port>/.well-known/oauth-protected-resource"
-- time
expired 2 hours ago -> 401, WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode the Jwt: Jwt expired at <time>", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1", resource_metadata="http://127.0.0.1:<port>/.well-known/oauth-protected-resource"
expired 30 seconds ago -> 200, {"user":"an","authorities":["FACTOR_BEARER","ROLE_USER"]}
expired 90 seconds ago -> 401, WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode the Jwt: Jwt expired at <time>", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1", resource_metadata="http://127.0.0.1:<port>/.well-known/oauth-protected-resource"
-- there is no revoke
login after the user was deleted -> 401
old token of the deleted user -> 200, {"user":"an","authorities":["FACTOR_BEARER","ROLE_USER"]}
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
| Token phát hành có thêm vai trò `FACTOR_PASSWORD` | Spring Security 7 thêm quyền yếu tố xác thực vào `Authentication` | Chỉ đưa các quyền bắt đầu bằng `ROLE_` vào token, như `AuthController` |
| Mọi yêu cầu 401 kèm `invalid_token` | Khóa ký ở bộ giải mã khác khóa phát hành | Dùng cùng `SecretKey` cho `JwtEncoder` và `JwtDecoder` |
