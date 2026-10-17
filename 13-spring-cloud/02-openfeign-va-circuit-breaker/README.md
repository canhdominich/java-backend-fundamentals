# Bài 02 - OpenFeign và circuit breaker

Mã nguồn đi kèm bài học **OpenFeign và Circuit Breaker** (chương 13: Spring Cloud) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một interface `PaymentClient` được `@FeignClient` biến thành client HTTP, cách lỗi HTTP trở thành ngoại lệ có kiểu (`NotFound`, `InternalServerError`), thời gian chờ mặc định (đo 60 giây ở lần đọc, còn lab này đặt 500 ms để thấy lỗi nhanh), việc `fallback` bị bỏ qua khi circuit breaker tắt, giới hạn thời gian 1 giây ngầm định khi bật circuit breaker, và vòng đời của mạch: đóng, mở sau 4 lời gọi lỗi (dịch vụ thanh toán không nhận thêm lời gọi nào), nửa mở sau khi chờ và đóng lại khi dịch vụ khỏe.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.0.8 | khai báo ở `parent` trong `pom.xml`; Spring Cloud 2025.1.3, OpenFeign 5.0.3 |
| Cổng mạng | cổng ngẫu nhiên | cả hai dịch vụ chạy trong cùng một tiến trình trên cổng ngẫu nhiên |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-openfeign-va-circuit-breaker/
  pom.xml                       parent Boot 4.0.8, BOM Spring Cloud 2025.1.3, OpenFeign, Resilience4j
  src/main/java/dev/canhdominich/cloud/
    payment/PaymentApplication.java   dịch vụ thanh toán giả lập
    payment/PaymentController.java    GET /payments/{orderId}, chế độ ok, slow, error, hang và bộ đếm lời gọi
    order/OrderApplication.java       @EnableFeignClients
    order/PaymentClient.java          interface @FeignClient có fallback
    order/PaymentFallback.java        giá trị thay thế pending-{id}
    order/OrderController.java        GET /orders/{id}/pay gọi PaymentClient
    FeignDemo.java                    chạy mọi thí nghiệm và in kết quả
  src/test/java/dev/canhdominich/cloud/
    FeignBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `FeignDemo` (đã bỏ log ở stderr; thời gian làm tròn tới 250 ms):

```text
-- a declarative client
client bean is a generated proxy: true
pay(7) returned: paid-7
hits on payment: 1
-- errors become exceptions
pay(404) threw: NotFound, status 404
payment down, pay(7) threw: InternalServerError, status 500
-- timeouts
default timeouts, payment answers in 2 s: paid-7 in 2000 ms
read-timeout 500 ms: RetryableException, status -1 in 500 ms
-- the fallback is ignored while the circuit breaker is off
fallback declared, breaker off, payment down: InternalServerError, status 500
-- circuit breaker on: the default time limit
healthy payment: paid-7 in under 50 ms
payment answers in 2 s: pending-7 in 1000 ms
-- circuit breaker on: opening, failing fast, recovering
failing call 1: pending-7 in under 50 ms, circuit [PaymentClientpaylong=CLOSED]
failing call 2: pending-7 in under 50 ms, circuit [PaymentClientpaylong=CLOSED]
failing call 3: pending-7 in under 50 ms, circuit [PaymentClientpaylong=CLOSED]
failing call 4: pending-7 in under 50 ms, circuit [PaymentClientpaylong=OPEN]
payment hits so far: 4
call 5 with the circuit open: pending-7 in under 50 ms, circuit [PaymentClientpaylong=OPEN]
call 6 with the circuit open: pending-7 in under 50 ms
payment hits after calls 5 and 6: 4
after 3.5 s with payment healthy: paid-7 in under 50 ms, circuit [PaymentClientpaylong=HALF_OPEN]
next call: paid-7 in under 50 ms, circuit [PaymentClientpaylong=CLOSED]
```

Để đo thời gian chờ đọc mặc định, đặt chế độ `hang` (dịch vụ ngủ 65 giây) và gọi `pay(1)` không cấu hình: ngoại lệ `RetryableException` (nguyên nhân `SocketTimeoutException: Read timed out`) xuất hiện sau 60 giây.

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
| Cột thời gian lệch (ví dụ 1250 ms thay vì 1000 ms) | Máy đang bận, lần gọi đầu có thêm thời gian khởi tạo | Chạy lại khi máy rảnh; các phép khẳng định của bộ test chỉ dựa vào mốc 500, 1000 và 2000 ms |
| `fallback` không bao giờ chạy | Chưa bật `spring.cloud.openfeign.circuitbreaker.enabled=true` | Bật thuộc tính và có starter circuit breaker trên classpath |
| Bản fallback chạy ngay cả khi dịch vụ chỉ chậm | Circuit breaker có giới hạn thời gian mặc định 1 giây | Cấu hình `resilience4j.timelimiter` cho phù hợp hoặc tăng thời gian đó |
