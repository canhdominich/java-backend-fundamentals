# Bài 02 - Assertions, kiểm thử tham số hóa và TDD

Mã nguồn đi kèm bài học **Assertions, Parameterized và TDD** (chương 15: JUnit) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: cùng một lỗi so sánh được JUnit và AssertJ mô tả bằng hai thông báo khác nhau (cả khi hai đối số bị đảo chỗ, và với số thực), `assertAll` báo mọi lỗi còn ba lệnh liền nhau dừng ở lỗi đầu, `assertThrows`; kiểm thử tham số hóa so với vòng lặp trong một test (một dòng hỏng không che các dòng khác), tên của từng lần gọi với `@CsvSource`, `@MethodSource`, `@EnumSource`, `@NullAndEmptySource`; và một vòng TDD đo được: cùng một bộ test chạy với bốn bản cài đặt từ "luôn trả 0" tới bản đúng, in ra số test đỏ ở mỗi bước (9, 7, 3, 0 trên 11).

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
02-assertions-parameterized-tdd/
  pom.xml                       junit-bom 6.0.3, junit-jupiter, junit-jupiter-params, launcher, AssertJ
  src/main/java/dev/canhdominich/junit/pricing/
    DiscountRules.java          giao diện: percent(quantity, member)
    DiscountPolicy.java         bản cài đặt đúng: 10 và 20 phần trăm theo số lượng, thêm 8 cho hội viên, trần 25
    PriceCalculator.java        tính tổng tiền từ tỉ lệ giảm
  src/test/java/dev/canhdominich/junit/pricing/
    DiscountContract.java       bộ test dùng chung cho mọi bản cài đặt (11 test)
    DiscountPolicyTest.java     chạy bộ test với DiscountPolicy thật
    Stages.java                 bốn bản cài đặt theo các bước TDD
    ProbeRunner.java            chạy một lớp bằng Launcher API
    ParameterizedProbes.java    các lớp thăm dò cho kiểm thử tham số hóa
    AssertionsTest.java         thông báo của các assertion
    ParameterizedMechanicsTest.java  quan sát kiểm thử tham số hóa
    TddStagesTest.java          đo vòng TDD
```

## Chạy thí nghiệm

```bash
mvn -q test -Dtest=AssertionsTest
mvn -q test -Dtest=ParameterizedMechanicsTest
mvn -q test -Dtest=TddStagesTest
```

## Kết quả mong đợi

Báo cáo in ra từ ba lớp, theo thứ tự trên:

```text
-- the same wrong string, two messages
   JUnit:   expected: <hello world> but was: <hello word>
   AssertJ: expected: "hello world" but was: "hello word"
-- two lists that differ in the last element
   JUnit:   expected: <[1, 2, 3]> but was: <[1, 2, 4]>
   AssertJ: Expecting actual: [1, 2, 4] to contain exactly (and in same order): [1, 2, 3] but some elements were not found: [3] and others were not expected: [4]
   with .as("cart lines"): [cart lines] expected: [1, 2, 3] but was: [1, 2, 4]
-- expected first, actual second
   assertEquals(5, actual):  expected: <5> but was: <4>
   assertEquals(actual, 5):  expected: <4> but was: <5>
-- 0.1 + 0.2
   exact comparison: expected: <0.3> but was: <0.30000000000000004>
   with a delta of 1e-9: passes
-- assertAll against three checks that all fail
   assertAll reports 3 failures
   three assertions in a row: stops after the first, 1 of 3 reached
-- assertThrows
   returns the exception, message: quantity must be positive: 0
   wrong type: Unexpected exception type thrown, expected: <java.lang.IllegalStateException> but was: <java.lang.IllegalArgumentException>
-- one row fails: parameterized test against a loop inside one test
   @ValueSource {2, 4, 6, 7, 8}: [1] 2 successful; [2] 4 successful; [3] 6 successful; [4] 7 failed; [5] 8 successful
   plain loop in one test: 1 result, numbers checked [2, 4, 6, 7]
-- @CsvSource with name = "{0} plus {1} is {2}"
   "2" plus "3" is "5"
   "10" plus "20" is "30"
   "-1" plus "1" is "0"
-- default names
   @MethodSource: [[1] "book", 15000, [2] "pen", 5000]
   @EnumSource: [[1] BASIC, [2] MEMBER, [3] VIP]
   @NullAndEmptySource plus one blank string: [[1] null, [2] "", [3] " "]
-- 1. Red: the contract against an implementation that always returns 0
   9 of 11 tests fail
     totalAppliesTheDiscount()
     quantity "10", member "false" gives "10" percent
     quantity "49", member "false" gives "10" percent
     quantity "50", member "false" gives "20" percent
     quantity "1", member "true" gives "8" percent
     quantity "10", member "true" gives "18" percent
     quantity "50", member "true" gives "25" percent
     quantity "100", member "true" gives "25" percent
     rejectsAQuantityThatIsNotPositive()
-- 2. Green for the first rule: 10 percent from quantity 10
   7 of 11 tests fail
     totalAppliesTheDiscount()
     quantity "50", member "false" gives "20" percent
     quantity "1", member "true" gives "8" percent
     quantity "10", member "true" gives "18" percent
     quantity "50", member "true" gives "25" percent
     quantity "100", member "true" gives "25" percent
     rejectsAQuantityThatIsNotPositive()
-- 3. All rules written, the cap forgotten
   3 of 11 tests fail
     totalAppliesTheDiscount()
     quantity "50", member "true" gives "25" percent
     quantity "100", member "true" gives "25" percent
-- 4. The real DiscountPolicy with the cap
   0 of 11 tests fail
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 24, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Tests run: 11` ở `DiscountPolicyTest` dù chỉ có ba phương thức | Mỗi dòng của `@CsvSource` là một lần gọi riêng | Đây là đúng hành vi: 9 dòng cộng hai test thường là 11 |
| Thông báo của AssertJ khác bài | Phiên bản AssertJ khác | Dùng 3.27.7 như `pom.xml`; các phép khẳng định của bộ test so với thông báo chính xác |
