# Bài 01 - JUnit cơ bản: vòng đời, kết quả và thứ tự

Mã nguồn đi kèm bài học **Giới thiệu JUnit và Annotations** (chương 15: JUnit) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một lớp `ShoppingCart` cùng các test viết bằng JUnit Jupiter (`@DisplayName`, `@Nested`, `@BeforeEach`, AssertJ), và một bộ "test của test": các lớp thăm dò (`Probes`) được chạy bằng Launcher API của JUnit để quan sát chính JUnit. Bạn đo vòng đời mặc định (một đối tượng mới cho mỗi phương thức test, và vì thế thứ tự khai báo không được bảo đảm), vòng đời `PER_CLASS`, sáu kết quả khác nhau (thành công, hỏng assertion, ném ngoại lệ, bỏ qua bằng `@Disabled`, giả định không thỏa, quá `@Timeout`), thứ tự chạy mặc định so với `@Order`, lọc theo `@Tag` và `@RepeatedTest`.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| JUnit | 6.0.3 | khai báo ở `junit-bom` trong `pom.xml` |
| AssertJ | 3.27.7 | thư viện assertion |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-junit-co-ban/
  pom.xml                       junit-bom 6.0.3, junit-jupiter, junit-platform-launcher, AssertJ
  src/main/java/dev/canhdominich/junit/shop/
    ShoppingCart.java           lớp được kiểm thử: add, total, itemCount, clear
  src/test/java/dev/canhdominich/junit/shop/
    ShoppingCartTest.java       năm test thông thường
    Probes.java                 các lớp thăm dò: vòng đời, kết quả, thứ tự, tag, lặp lại
    ProbeRunner.java            chạy một lớp thăm dò bằng Launcher API và thu kết quả
    MechanicsTest.java          bảy test khẳng định điều quan sát được và in báo cáo
```

## Chạy thí nghiệm

```bash
mvn -q test -Dtest=MechanicsTest
```

## Kết quả mong đợi

Báo cáo in ra từ `MechanicsTest`:

```text
-- default lifecycle: a new instance for every test method
   beforeAll
   constructor instance A
   beforeEach on instance A
   test second on instance A
   afterEach on instance A
   constructor instance B
   beforeEach on instance B
   test first on instance B
   afterEach on instance B
   afterAll
-- PER_CLASS lifecycle: one instance for the whole class
   constructor instance A
   beforeAll on instance A
   test second on instance A
   test first on instance A
-- six tests: four statuses, three kinds of failure
   assumptionIsNotMet()   ABORTED    TestAbortedException
   failsAnAssertion()     FAILED     AssertionFailedError
   isDisabled()           SKIPPED    not ready
   passes()               SUCCESSFUL 
   takesTooLong()         FAILED     TimeoutException
   throwsAnException()    FAILED     NullPointerException
-- default method order (declared zulu, alpha, mike, bravo)
   mike, zulu, alpha, bravo
-- with @TestMethodOrder and @Order
   alpha, mike, zulu
-- tags
   without a filter: 4 tests; only tag smoke: [smokeOne(), smokeTwo()]
-- @RepeatedTest(3)
   [repetition 1 of 3, repetition 2 of 3, repetition 3 of 3]
```

Thứ tự mặc định `mike, zulu, alpha, bravo` là thứ tự xác định nhưng không có ý nghĩa; JUnit tính nó từ tên phương thức nên bạn thấy giống nhau ở mỗi lần chạy, nhưng đừng dựa vào nó.

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 12, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Tests run: 0` ở dòng của lớp `@Nested` | Surefire in một dòng cho lớp ngoài và một dòng cho mỗi lớp lồng | Đọc dòng tổng cuối cùng `Tests run: 12` |
| Test thăm dò chạy cùng bộ test và làm bộ test đỏ | Lớp thăm dò có tên khớp mẫu của Surefire (`*Test`) | Đặt tên không kết thúc bằng `Test` và để thành lớp lồng tĩnh như `Probes` |
