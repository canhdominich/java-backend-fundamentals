# Bài 04 - Actuator, DevTools và logging

Mã nguồn đi kèm bài học **Actuator, DevTools và Logging** (chương 8: Spring Boot) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: Actuator với cấu hình mặc định (chỉ `health` được mở qua HTTP) rồi mở thêm `metrics`, `env`, `loggers`; `health` có chi tiết cùng một `HealthIndicator` tự viết làm dịch vụ chuyển sang `DOWN` (503); một chỉ số Micrometer tự đặt (`orders.created`); `env` che giá trị theo mặc định và hậu quả của `show-values=ALWAYS`; đổi mức log khi ứng dụng đang chạy qua `/actuator/loggers`; log có cấu trúc dạng JSON (`logstash`); và DevTools khởi động lại bằng `RestartClassLoader` khi chạy từ thư mục lớp nhưng không có mặt trong executable jar.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
04-actuator-devtools-logging/
  pom.xml                       starter webmvc, actuator, devtools (optional), test
  src/main/java/dev/canhdominich/boot/ops/
    OpsApplication.java         @SpringBootApplication
    WorkQueue.java              hàng đợi giả có độ sâu chỉnh được
    QueueHealthIndicator.java   HealthIndicator tự viết: DOWN khi độ sâu vượt 1000
    OrderController.java        POST /orders tăng bộ đếm và ghi log, POST /queue đổi độ sâu
    LoaderController.java       GET /loader trả tên class loader của lớp ứng dụng
    OpsDemo.java                chạy mọi thí nghiệm và in kết quả
  src/main/resources/
    application.properties      app.name và app.password dùng cho thí nghiệm env
  src/test/java/dev/canhdominich/boot/ops/
    OpsBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

Để xem DevTools trong executable jar:

```bash
mvn -q package -DskipTests
java -jar target/boot-ops-1.0.0.jar --server.port=8080
curl http://localhost:8080/loader
```

## Kết quả mong đợi

Kết quả của `OpsDemo` (đã bỏ log ở stderr):

```text
-- default exposure
GET /actuator -> 200 links [health, self]
GET /actuator/health -> 200 {"groups":["liveness","readiness"],"status":"UP"}
GET /actuator/metrics -> 404
GET /actuator/env -> 404
-- exposed: health with details, metrics, env, loggers
GET /actuator/health -> 200 components [diskSpace, livenessState, ping, queue, readinessState, ssl]
queue component: "queue":{"details":{"depth":0,"limit":1000},"status":"UP"}
GET /actuator/metrics/orders.created after 3 orders -> "value":3.0
jvm.memory.used is a known meter: true
env app.name -> "property":{"source":"Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'","value":"******"}
env app.password -> "property":{"source":"Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'","value":"******"}
queue depth 5000 -> GET /actuator/health 503 "queue":{"details":{"depth":5000,"limit":1000},"status":"DOWN"}
-- env values with show-values=ALWAYS
env app.name -> "value":"Course Platform"
env app.password -> "value":"hunter2"
-- changing a log level while the application runs
default level: [INFO order accepted]
POST /actuator/loggers/dev.canhdominich.boot.ops.OrderController DEBUG -> 204
after DEBUG: [DEBUG order accepted, details for debugging, INFO order accepted]
-- structured logging in a separate process
logstash format, keys of one line: @timestamp, @version, level, level_value, logger_name, message, thread_name
-- devtools
class loader of the application classes -> RestartClassLoader
```

Khi chạy bằng `java -jar` (lệnh ở trên), `GET /loader` trả `LaunchedClassLoader` và `unzip -l target/boot-ops-1.0.0.jar | grep -c devtools` cho `0`: `spring-boot-maven-plugin` không đưa DevTools vào jar. Trong `mvn test`, dòng `class loader of the application classes` là `AppClassLoader` vì DevTools tự tắt khi chạy kiểm thử.

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
| `GET /actuator/metrics` trả 404 | Mặc định chỉ `health` được mở qua HTTP | Thêm `management.endpoints.web.exposure.include=health,metrics` |
| Port đã bị chiếm khi chạy jar | Cổng 8080 đang dùng | `--server.port=8081` |
