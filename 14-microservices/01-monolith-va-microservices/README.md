# Bài 01 - Monolith và microservices: đo cái giá của việc tách

Mã nguồn đi kèm bài học **Microservices là gì? So sánh Monolithic và Microservices** (chương 14: Microservices) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: cùng một nghiệp vụ đặt hàng (trừ tồn kho rồi lưu đơn) viết hai cách, một ứng dụng nguyên khối với một giao dịch và hai dịch vụ `inventory` và `order` mỗi dịch vụ một cơ sở dữ liệu riêng gọi nhau bằng HTTP. Bạn đo bộ nhớ và thời gian khởi động của mỗi cách, độ trễ của từng lời gọi, điều xảy ra khi đơn hàng lỗi sau bước trừ kho (nguyên khối hoàn tác cả hai, microservices để lại kho bị trừ mà không có đơn), và điều xảy ra khi dịch vụ tồn kho dừng (đặt hàng lỗi nhưng dịch vụ đơn hàng vẫn đọc được dữ liệu của chính nó). Mỗi dịch vụ chạy trong một tiến trình Java riêng với cùng tùy chọn `-Xmx128m -XX:+UseSerialGC`.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml` |
| Bộ nhớ trống | khoảng 1 GB | ba tiến trình Java chạy cùng lúc |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-monolith-va-microservices/
  pom.xml                       parent Boot 4.1.1, webmvc, jdbc, restclient, H2
  src/main/java/dev/canhdominich/micro/
    monolith/MonolithApplication.java     nguyên khối: bảng stock và orders cùng một cơ sở dữ liệu
    monolith/ShopService.java             placeOrder trong một giao dịch @Transactional
    monolith/ShopController.java          POST /orders, GET /stock/{sku}, GET /orders/count
    inventory/InventoryApplication.java   dịch vụ tồn kho với cơ sở dữ liệu riêng
    inventory/InventoryController.java    POST /reserve, GET /stock/{sku}
    order/OrderApplication.java           dịch vụ đơn hàng với cơ sở dữ liệu riêng
    order/OrderController.java            POST /orders gọi inventory rồi lưu đơn, GET /orders/count
    ComparisonDemo.java                   khởi chạy từng tiến trình và đo
  src/test/java/dev/canhdominich/micro/
    ComparisonBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

Chương trình mất khoảng 40 giây vì nó khởi động ba tiến trình Java. Nhật ký từng tiến trình nằm ở `target/logs/`.

## Kết quả mong đợi

Kết quả của `ComparisonDemo` (bộ nhớ làm tròn tới 10 MB, thời gian làm tròn tới 0,5 giây):

```text
-- cost of running it
processes: monolith 1, microservices 2
memory after warm-up: monolith 200 MB, microservices 440 MB
time until ready: monolith 6.0 s, microservices (started one after the other) 14.5 s
-- cost of every call
median of 300 orders: monolith 5 ms, microservices 10 ms
one extra network hop makes the median slower: true
-- an order that fails after the stock was reserved
monolith: status 500, stock change 0, orders change 0
microservices: status 500, stock change -5, orders change 0
-- inventory service stops
order service, placing an order: status 500
order service, reading its own data: status 200
```

Các con số về bộ nhớ, thời gian khởi động và độ trễ phụ thuộc máy bạn; thứ tự (microservices tốn hơn nguyên khối) và các dòng về hoàn tác, trừ kho và trạng thái 500 và 200 phải y hệt.

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
| `did not start, see target/logs` | Một tiến trình không khởi động trong 90 giây (máy chậm hoặc cổng bị chặn) | Mở `target/logs/<tên>.log` để xem lỗi, rồi chạy lại |
| Con số độ trễ hoặc bộ nhớ khác bài | Máy, số lõi và bộ nhớ khác nhau | So thứ tự giữa hai cách chứ không so con số tuyệt đối |
