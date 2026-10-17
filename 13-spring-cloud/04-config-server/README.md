# Bài 04 - Config Server và cấu hình tập trung

Mã nguồn đi kèm bài học **Config Server và cấu hình tập trung** (chương 13: Spring Cloud) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một Config Server đọc một thư mục tệp cấu hình (kiểu `native`, không cần Git), cách nó trộn `application.yml`, `orders.yml` và `orders-prod.yml` theo độ cụ thể, một client lấy cấu hình qua `spring.config.import`, việc đổi tệp rồi `POST /actuator/refresh` (chỉ bean có `@RefreshScope` thấy giá trị mới), thứ tự ưu tiên giữa cấu hình từ server với cấu hình cục bộ, mã hóa giá trị bí mật bằng `{cipher}`, và điều xảy ra khi server không chạy lúc client khởi động (`optional:` hay bắt buộc).

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.0.8 | khai báo ở `parent` trong `pom.xml`; Spring Cloud 2025.1.3, Config 5.0.5 |
| Cổng mạng | cổng ngẫu nhiên | server và client chạy trong cùng một tiến trình trên cổng ngẫu nhiên |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
04-config-server/
  pom.xml                       parent Boot 4.0.8, BOM Spring Cloud 2025.1.3, config server, config client, actuator
  src/main/java/dev/canhdominich/cloud/
    server/ConfigServerApplication.java   @EnableConfigServer
    client/OrdersApplication.java         ứng dụng client
    client/PolicyController.java          GET /policy, @RefreshScope
    client/StaticPolicyController.java    GET /static-policy, không có @RefreshScope
    ConfigDemo.java                       tạo thư mục cấu hình tạm, chạy server và các client, in kết quả
  src/test/java/dev/canhdominich/cloud/
    ConfigBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `ConfigDemo` (đã bỏ log ở stderr):

```text
-- what the server serves
GET /orders/default property sources: [orders.yml, application.yml]
GET /orders/prod property sources: [orders-prod.yml, orders.yml, application.yml]
-- a client reads its configuration at startup
profile default: maxItems=10 currency=VND
-- the repository changes
server now serves max-items: 25
client before refresh: maxItems=10 currency=VND
POST /actuator/refresh returns: [orders.max-items]
client after refresh: maxItems=25 currency=VND
bean without @RefreshScope after refresh: maxItems=10
profile prod: maxItems=50 currency=VND
-- local settings against the server
local default property orders.max-items=99: maxItems=10 currency=VND
command-line argument --orders.max-items=99: maxItems=99 currency=VND
-- secrets
encrypted value starts with a hex string: true
server answers with the decrypted value: s3cret-value
-- the server is not reachable at startup
optional import: started with maxItems=unset currency=unset
required import: failed to start, ConfigClientFailFastException
```

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
| `Incorrect ConfigDataLocationResolver chosen` khi client khởi động | Config server và client cùng classpath nên import `configserver:` không được bật mặc định | Đặt `spring.cloud.config.enabled=true` cho client, như `ConfigDemo` làm |
| `POST /actuator/refresh` trả 415 | Yêu cầu thiếu `Content-Type` | Gửi `Content-Type: application/json` với thân `{}` |
| `POST /encrypt` trả 400 | Thiếu `Content-Type: text/plain` trong yêu cầu | Gửi `Content-Type: text/plain` với thân là giá trị cần mã hóa |
