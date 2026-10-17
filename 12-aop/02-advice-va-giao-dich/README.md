# Bài 02 - Các loại advice và quản lý giao dịch

Mã nguồn đi kèm bài học **Các loại Advice và quản lý giao dịch** (chương 12: AOP) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: năm loại advice quanh một lời gọi thành công và một lời gọi ném lỗi (`@AfterReturning` chỉ khi thành công, `@AfterThrowing` chỉ khi lỗi, `@After` luôn chạy), những việc chỉ `@Around` làm được (đổi kết quả, bỏ qua lời gọi đích để làm cache), danh sách advice trên proxy cho thấy `@Transactional` cũng là một advice (`TransactionInterceptor`) xếp cùng chuỗi với aspect của bạn, và hệ quả của thứ tự: aspect thử lại đặt ngoài giao dịch tạo giao dịch mới mỗi lần thử (1 dòng được lưu), đặt trong giao dịch thì giữ ghi của các lần thử thất bại (3 dòng).

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml` |
| H2 | do Boot quản lý | cơ sở dữ liệu trong bộ nhớ, không cần Docker |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-advice-va-giao-dich/
  pom.xml                       parent Boot 4.1.1, starter aspectj, jdbc, h2, test
  src/main/java/dev/canhdominich/boot/aop/
    AdviceApplication.java      @SpringBootApplication, @EnableTransactionManagement(order = 200)
    Events.java                 ghi lại sự kiện theo thứ tự
    Traced.java, Capped.java, Cached.java, RetryOutside.java, RetryInside.java   năm annotation đánh dấu
    PaymentService.java         phương thức để thử năm loại advice, đổi kết quả và cache
    AdviceAspect.java           @Around, @Before, @AfterReturning, @AfterThrowing, @After, cap và cache (thứ tự 50)
    FlakyWriter.java            hai phương thức @Transactional ghi một dòng rồi thất bại hai lần đầu
    RetryAspects.java           hai aspect thử lại: thứ tự 100 (ngoài giao dịch) và 300 (trong giao dịch)
    AdviceDemo.java             chạy mọi thí nghiệm
  src/main/resources/
    schema.sql                  bảng saved dùng cho thí nghiệm thử lại
  src/test/java/dev/canhdominich/boot/aop/
    AdviceBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `AdviceDemo` (đã bỏ log ở stderr):

```text
-- the five kinds of advice, success and failure
charge(100) returned charged 100
    around: before
    before
    body: charge 100
    after returning: charged 100
    after (finally)
    around: after
charge(0) threw IllegalStateException: amount must be positive
    around: before
    before
    body: charge 0
    after throwing: amount must be positive
    after (finally)
    around: after
-- what @Around can do that the others cannot
changes the result: RECEIPT-12345
skips the call: priceOf twice gave 4000 and 4000, target ran 1 time(s)
-- @Transactional is an aspect too
advice objects on the FlakyWriter proxy: [ExposeInvocationInterceptor, AspectJAroundAdvice, TransactionInterceptor, AspectJAroundAdvice]
-- retry outside the transaction (aspect order 100, transactions at 200)
saveWithRetryOutsideTransaction returned saved
    retry outside the transaction: attempt 1, transaction active = false
    retry outside the transaction: attempt 1 failed
    retry outside the transaction: attempt 2, transaction active = false
    retry outside the transaction: attempt 2 failed
    retry outside the transaction: attempt 3, transaction active = false
rows saved: 1
-- retry inside the transaction (aspect order 300)
saveWithRetryInsideTransaction returned saved
    retry inside the transaction: attempt 1, transaction active = true
    retry inside the transaction: attempt 1 failed
    retry inside the transaction: attempt 2, transaction active = true
    retry inside the transaction: attempt 2 failed
    retry inside the transaction: attempt 3, transaction active = true
rows saved: 3
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
| Dòng `rows saved` cho kết quả khác bài | Thứ tự của hai aspect thử lại không khớp thứ tự giao dịch | Giữ `@EnableTransactionManagement(order = 200)` và `@Order` 100 và 300 |
| Aspect thử lại không chạy | Pointcut theo annotation không khớp | Giữ annotation `RetryOutside` hoặc `RetryInside` trên phương thức |
