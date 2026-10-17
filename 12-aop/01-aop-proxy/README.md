# Bài 01 - AOP trong Spring: aspect và proxy

Mã nguồn đi kèm bài học **Aspect-Oriented Programming trong Spring** (chương 12: AOP) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: bean được tăng cường (advised) bị thay bằng proxy CGLIB (`OrderService$$SpringCGLIB$$0`) và bằng proxy JDK khi tắt `proxy-target-class` và có interface, thứ tự thật của `@Around`, `@Before`, `@AfterReturning`, `@After` quanh một phương thức có `@Audited`, thứ tự lồng của hai aspect theo `@Order`, gọi qua `this` bỏ qua aspect, phương thức `final` không được tăng cường và còn nhận trạng thái rỗng của proxy (`nullbook`), và `getBean` theo lớp cụ thể thất bại khi bean là proxy JDK.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml`; quản lý Spring Framework 7.0.9 và AspectJ |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-aop-proxy/
  pom.xml                       parent Boot 4.1.1, starter aspectj, test
  src/main/java/dev/canhdominich/boot/aop/
    AopApplication.java         @SpringBootApplication
    Events.java                 ghi lại các sự kiện theo thứ tự
    Audited.java                annotation đánh dấu phương thức cần ghi kiểm toán
    OrderService.java           lớp có phương thức public, @Audited, gọi nội bộ, final và private
    PriceCalculator.java, PricingService.java   interface và cài đặt để so proxy JDK với CGLIB
    LoggingAspect.java          @Around, @Before, @AfterReturning, @After (thứ tự 2)
    OuterAspect.java            aspect bên ngoài (thứ tự 1)
    AopDemo.java                chạy mọi thí nghiệm
  src/test/java/dev/canhdominich/boot/aop/
    AopBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `AopDemo` (đã bỏ log ở stderr; số thứ tự của proxy JDK được thay bằng `<n>`):

```text
-- what the beans really are
OrderService class: OrderService$$SpringCGLIB$$0 (CGLIB proxy, target OrderService)
PriceCalculator class: PricingService$$SpringCGLIB$$0 (CGLIB proxy, target PricingService)
-- advice around a public method with @Audited
place returned order-book
    around: before place
    before (audited): place
    body: place book
    body: private secret book
    after returning: place -> order-book
    after (finally): place
    around: after place
-- a public method without @Audited
cancel returned cancelled book
    outer aspect: before cancel
    around: before cancel
    body: cancel book
    around: after cancel
    outer aspect: after cancel
-- a method that calls another through this
outer returned order-book
    around: before outer
    body: outer book
    body: place book
    body: private secret book
    around: after outer
-- a final method
receipt returned nullbook
    body: receipt book
-- an interface-based bean that an aspect advises
priceOf returned 4000
    before: priceOf
-- the same beans with spring.aop.proxy-target-class=false
PriceCalculator class: $Proxy<n> (JDK dynamic proxy, target PricingService)
OrderService class (no interface): OrderService$$SpringCGLIB$$0 (CGLIB proxy, target OrderService)
priceOf returned 4000
    before: priceOf
getBean(PricingService.class): NoSuchBeanDefinitionException
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
| Aspect không chạy | Thiếu `spring-boot-starter-aspectj` hoặc bean không phải bean Spring | Giữ starter trong `pom.xml`; bean phải do container quản lý |
| `getBean(PricingService.class)` báo không tìm thấy | Bean là proxy JDK chỉ có kiểu interface | Tiêm theo interface, hoặc giữ `proxy-target-class=true` |
