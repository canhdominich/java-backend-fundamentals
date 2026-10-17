# Bài 02 - Nhất quán giữa các dịch vụ: bù trừ và idempotency

Mã nguồn đi kèm bài học **Giao tiếp và nhất quán dữ liệu: saga và idempotency** (chương 14: Microservices) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: hai dịch vụ `inventory` và `order` (mỗi dịch vụ một cơ sở dữ liệu) và bốn cách làm cho dữ liệu nhất quán trở lại khi không còn giao dịch chung: bù trừ (dịch vụ đơn hàng gọi `release` khi một bước sau lỗi), khóa idempotency (cùng khóa đặt hai lần chỉ trừ kho một lần, nên thử lại khi hết thời gian chờ là an toàn), bù trừ cả khi không biết kết quả của lời gọi, và hàng đợi bù trừ chờ gửi lại khi chính lời gọi bù trừ thất bại. Mỗi kịch bản chạy trên một cặp dịch vụ mới, cùng một tiến trình trên cổng ngẫu nhiên.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml` |
| Cổng mạng | cổng ngẫu nhiên | hai dịch vụ chạy trong cùng một tiến trình |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-saga-va-idempotency/
  pom.xml                       parent Boot 4.1.1, webmvc, jdbc, restclient, H2
  src/main/java/dev/canhdominich/micro/
    inventory/InventoryApplication.java   bảng stock và reservations
    inventory/StockService.java           reserve và release có khóa idempotency, @Transactional
    inventory/InventoryController.java    /reserve, /release, /stock/{sku}, /admin/delay, /admin/release-down
    order/OrderApplication.java           bảng orders
    order/OrderService.java               đặt hàng kiểu saga: thử lại, bù trừ, hàng đợi bù trừ
    order/OrderController.java            /orders, /compensations/retry, /compensations/pending
    SagaDemo.java                         chạy từng kịch bản và in kết quả
  src/test/java/dev/canhdominich/micro/
    SagaBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `SagaDemo` (đã bỏ log ở stderr):

```text
-- failure after the stock was reserved
without compensation: status 500, stock change -5, orders 0
with compensation: status 500, stock change 0, orders 0
-- the first response is slow, the caller times out and retries
retry without a key: status 200, stock change -10, orders 1
retry with a key: status 200, stock change -5, orders 1
-- every response is slow, so every attempt times out
keyed reserve, unknown outcome, compensated: status 500, stock change 0, orders 0
-- the compensation itself cannot be delivered
release is down: status 500, stock change -5, orders 0, pending compensations 1
compensations delivered by the retry: 1
after the retry: stock change 0, pending compensations 0
-- the compensation is repeated
the order service already released it; releasing by hand: already released, stock change 0
releasing a key that never reserved: nothing to release
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 7, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Kịch bản có chậm 1,5 giây bị lệch kết quả | Thuộc tính `spring.http.clients.read-timeout=500ms` không áp dụng (không có `RestClient.Builder` của Boot) | Tiêm `RestClient.Builder` thay vì tự gọi `RestClient.create()` |
| Chương trình không in gì | Một ngữ cảnh khởi động lỗi nhưng log đã tắt | Bỏ `logging.level.root=OFF` để thấy lỗi; `SagaDemo` in nguyên nhân gốc |
