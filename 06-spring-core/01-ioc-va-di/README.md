# Bài 01 - IoC và Dependency Injection

Mã nguồn đi kèm bài học **Spring Framework và IoC** (chương 6: Spring Core) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một `OrderService` tự tạo phụ thuộc bằng `new` (và hậu quả khi kiểm thử), cùng `OrderService` nhận phụ thuộc qua constructor từ một container Spring (thứ tự tạo bean, đồ thị phụ thuộc, singleton), một lần kiểm thử không cần container bằng đối tượng giả, và danh sách các module Spring Core thật sự có trên classpath.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Framework | 7.0.9 | khai báo trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-ioc-va-di/
  pom.xml
  src/main/java/dev/canhdominich/spring/ioc/
    Events.java                  ghi lại và in các sự kiện (tạo bean, gọi thanh toán)
    PaymentService.java          interface thanh toán
    PaypalPayment.java           cài đặt thật, là một @Component
    FakePayment.java             bản giả dùng khi kiểm thử, không phải bean
    HardWiredOrderService.java   dịch vụ tự tạo phụ thuộc bằng new
    OrderService.java            dịch vụ nhận phụ thuộc qua constructor, là một @Service
    AppConfig.java               lớp cấu hình bật quét component
    IocDemo.java                 ba thí nghiệm: tự tạo, container, bản giả
    ModulesDemo.java             liệt kê các jar spring trên classpath
  src/test/java/dev/canhdominich/spring/ioc/
    IocBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.ioc.ModulesDemo
```

## Kết quả mong đợi

Lệnh đầu chạy `IocDemo` (lớp chính mặc định trong `pom.xml`):

```text
-- without a container: OrderService creates its own payment
  event: constructing PaypalPayment
  event: PAYPAL charged 100
result: paypal:100
-- with a container: the container creates and wires the beans
  event: constructing PaypalPayment
  event: constructing OrderService with PaypalPayment
bean names: [appConfig, orderService, paypalPayment]
orderService depends on: [paypalPayment]
paypalPayment is needed by: [orderService]
same instance twice: true
  event: PAYPAL charged 100
result: paypal:100
-- no container at all: a fake payment goes through the constructor
  event: constructing OrderService with FakePayment
result: fake:100, charges recorded by the fake: [100]
```

`ModulesDemo`:

```text
spring jars on the classpath:
  spring-aop-7.0.9.jar
  spring-beans-7.0.9.jar
  spring-context-7.0.9.jar
  spring-core-7.0.9.jar
  spring-expression-7.0.9.jar
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 4, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `mvn` báo không tìm thấy `jakarta.annotation` | Thiếu thư viện khai báo trong `pom.xml` | Chạy lại với mạng, hoặc xóa `~/.m2/repository/jakarta/annotation` rồi chạy lại |
| Không thấy dòng `event:` nào | Chạy sai lớp chính | Dùng đúng lệnh `mvn -q compile exec:exec` |
