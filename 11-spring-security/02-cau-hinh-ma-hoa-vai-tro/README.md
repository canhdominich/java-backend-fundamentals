# Bài 02 - Cấu hình bảo mật, mã hóa mật khẩu và vai trò

Mã nguồn đi kèm bài học **Cấu hình bảo mật, mã hóa và vai trò** (chương 11: Spring Security) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một `SecurityFilterChain` tự cấu hình (`permitAll`, `hasRole`, `anyRequest().authenticated()`) và ma trận người gọi theo đường dẫn (401 cho chưa xác thực, 403 cho đã xác thực nhưng thiếu vai trò, mật khẩu sai bị từ chối cả ở đường dẫn công khai), người dùng lưu trong cơ sở dữ liệu bằng `JdbcUserDetailsManager`, `PasswordEncoder` kiểu `DelegatingPasswordEncoder` (tiền tố `{bcrypt}`, muối khiến cùng mật khẩu ra hai chuỗi khác nhau, lưu không tiền tố bị từ chối), chi phí của BCrypt theo tham số độ mạnh, phân quyền theo phương thức bằng `@PreAuthorize` kèm kiểm tra chủ sở hữu, bỏ CSRF chỉ cho `/api/**`, thời gian đăng nhập của người dùng không tồn tại so với sai mật khẩu, và quy tắc đặt sau `anyRequest()` bị từ chối lúc khởi động.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml`; quản lý Spring Security 7.1.1 |
| H2 | do Boot quản lý | cơ sở dữ liệu trong bộ nhớ, không cần Docker |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-cau-hinh-ma-hoa-vai-tro/
  pom.xml                       parent Boot 4.1.1, starter security, jdbc, h2, test
  src/main/java/dev/canhdominich/boot/
    security/SecurityApplication.java    @SpringBootApplication
    security/SecurityConfig.java         SecurityFilterChain, PasswordEncoder, JdbcUserDetailsManager với hai người dùng
    security/Endpoints.java              endpoint công khai, cần đăng nhập, chỉ ADMIN, kiểm tra chủ sở hữu
    security/ConfiguredSecurityDemo.java chạy mọi thí nghiệm
    broken/BrokenApplication.java, BrokenConfig.java   cấu hình đặt quy tắc sau anyRequest() để thấy lỗi
  src/main/resources/
    schema.sql                  hai bảng users và authorities của Spring Security
  src/test/java/dev/canhdominich/boot/security/
    ConfiguredSecurityTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `ConfiguredSecurityDemo` (đã bỏ log ở stderr; số mili giây của BCrypt phụ thuộc máy, còn tỉ lệ giữa các dòng thì ổn định):

```text
-- who may call what (rows: caller, columns: endpoint)
no credentials         /public/info 200 | /api/me 401 | /admin/stats 401
an (USER)              /public/info 200 | /api/me 200 | /admin/stats 403
admin (ADMIN, USER)    /public/info 200 | /api/me 200 | /admin/stats 200
-- method security: owner check on /api/accounts/{owner}
an reads accounts/an -> 200
an reads accounts/binh -> 403
admin reads accounts/binh -> 200
-- CSRF ignored for /api only
POST /api/orders with Basic, no token -> 200
-- how passwords are stored
stored value starts with: {bcrypt}$2a (length 68)
same password encoded twice gives different values: true; both match: true
wrong password matches: false
explicit {noop} prefix still matches: true
stored without a prefix -> IllegalArgumentException: Given that there is no default password encoder configured, each password must have a password encoding prefix. Please either prefix this password with '{noop}' or set a default password encoder in `DelegatingPasswordEncoder`.
-- cost of one BCrypt hash
strength 4: 1 ms
strength 10: 92 ms
strength 12: 369 ms
strength 14: 1550 ms
-- login timing: unknown user against known user with a wrong password
unknown user: 102 ms, known user with a wrong password: 98 ms, similar: true
-- a misplaced rule is rejected at startup
startup failed: IllegalStateException: Can't configure requestMatchers after anyRequest
```

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
| Lỗi `Table "USERS" not found` lúc khởi động | `schema.sql` chạy sau khi bean người dùng được tạo | Giữ `@DependsOnDatabaseInitialization` trên bean `JdbcUserDetailsManager` |
| Thời gian BCrypt khác bài | Tốc độ CPU | So tỉ lệ: mỗi bậc độ mạnh nhân đôi thời gian |
