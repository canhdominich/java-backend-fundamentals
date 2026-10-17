# Bài 03 - Mockito: mock, stub, verify và những chỗ mock đánh lừa bạn

Mã nguồn đi kèm bài học **Mockito và test đôi** (chương 15: JUnit) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: bốn test viết kiểu thật cho `OrderService` (cô lập khỏi cổng thanh toán, kho và thông báo bằng `@Mock`, `when`, `verify`, `ArgumentCaptor`, `verifyNoInteractions`) và một bộ quan sát cơ chế của Mockito: mock chưa lập trình trả về giá trị rỗng (kể cả `Optional.empty`), thông báo của `verify` sai số lần, chế độ strict báo stub không được dùng (`UnnecessaryStubbingException`), `spy` gọi phương thức thật, mock lớp `final`, và một lỗi (quên thuế) lọt qua `verify` lỏng với `anyString()` và `anyLong()` nhưng bị `verify` chính xác bắt được.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| JUnit | 6.0.3 | khai báo ở `junit-bom` trong `pom.xml` |
| Mockito | 5.23.0 | mock được cả lớp `final` |
| AssertJ | 3.27.7 | thư viện assertion |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
03-mockito/
  pom.xml                       junit-bom 6.0.3, mockito-core 5.23.0, mockito-junit-jupiter, AssertJ
  src/main/java/dev/canhdominich/junit/orders/
    PaymentGateway.java, StockRepository.java, Notifier.java   ba phụ thuộc (giao diện)
    TaxTable.java               lớp final trả tỉ lệ thuế theo quốc gia
    OrderService.java           đặt hàng: kiểm tra kho, tính thuế, thanh toán, giữ hàng, thông báo
  src/test/java/dev/canhdominich/junit/orders/
    OrderServiceTest.java       bốn test viết kiểu thật với Mockito
    Probes.java                 lớp thăm dò: strict, lenient, bản có lỗi quên thuế, verify lỏng và chính xác
    ProbeRunner.java            chạy một lớp bằng Launcher API
    MockitoMechanicsTest.java   bảy test quan sát cơ chế và in báo cáo
```

## Chạy thí nghiệm

```bash
mvn -q test -Dtest=MockitoMechanicsTest
```

## Kết quả mong đợi

Báo cáo in ra từ `MockitoMechanicsTest`:

```text
-- a mock you have not stubbed
   int: 0, String: null, List.size(): 0, List.get(0): null
   a method declared to return Optional gives: Optional.empty (not null)
-- verify with the wrong count
   the message says "Wanted 2 times" and "But was 1 time": true
   and it names the call that did happen: true
-- a stub that the code under test never used
   MockitoExtension default (strict): FAILED UnnecessaryStubbingException
   strictness LENIENT: SUCCESSFUL
-- spy: a real object with a watcher
   after add on the spy: size 1, the real list size 0
   after doReturn(99).when(spy).size(): size 99, content [one]
-- a final class
   TaxTable is final; the mock answers 5, the real class answers 10
-- the same bug (tax forgotten) against two ways of verifying
   verify(payments).charge(anyString(), anyLong()):  correct code SUCCESSFUL, buggy code SUCCESSFUL
   verify(payments).charge(eq("An"), eq(22_000L)): correct code SUCCESSFUL, buggy code FAILED ArgumentsAreDifferent
-- a mock says what you made it say
   the mock returns tx-9 for exactly that call and null for any other
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 11, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `UnnecessaryStubbingException` trong test của bạn | `MockitoExtension` mặc định bật chế độ strict và bạn có stub mà mã không dùng | Xóa stub thừa, hoặc đặt `@MockitoSettings(strictness = LENIENT)` có chủ đích |
| `NullPointerException` ngay lúc dùng mock | Mock chưa được lập trình và trả `null` cho kiểu đối tượng | Lập trình bằng `when(...).thenReturn(...)` hoặc dùng bản giả (fake) |
