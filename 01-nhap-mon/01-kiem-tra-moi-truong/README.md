# Bài 01 - Kiểm tra môi trường học

Mã nguồn đi kèm bài học **Advanced Java là gì?** (chương 1: Giới thiệu Advanced Java) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một chương trình nhỏ `EnvCheck` kiểm tra máy của bạn có đủ điều kiện làm các bài thực hành của khóa học không (JDK 17 trở lên, bảng mã mặc định, bộ nhớ, Maven, Docker, cổng MySQL của các bài JDBC) và in `PASS`, `WARN` hoặc `FAIL` cho từng mục, thoát với mã 1 nếu có `FAIL`. Chạy nó một lần trước khi vào chương JDBC.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Cổng 3307 | tùy chọn | MySQL của các bài JDBC; `WARN` nếu chưa chạy |
| Docker | bất kỳ bản gần đây | không bắt buộc cho bài này; các bài JDBC, kiểm thử và microservices cần tới |

## Cấu trúc

```text
01-kiem-tra-moi-truong/
  pom.xml                       Maven thuần, release 17, JUnit và AssertJ cho test
  src/main/java/dev/canhdominich/intro/env/
    EnvCheck.java               sáu phép kiểm tra và mã thoát
  src/test/java/dev/canhdominich/intro/env/
    EnvCheckTest.java           ba test
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả trên máy đã kiểm chứng (phiên bản của bạn có thể khác, miễn là các dòng không phải `FAIL`):

```text
PASS  JDK 17.0.15+6-Ubuntu-0ubuntu120.04 (Ubuntu), the course needs 17 or newer
PASS  default charset UTF-8, native encoding UTF-8
PASS  maximum heap 3972 MB, processors 4
PASS  Apache Maven 3.9.12 (848fbb4bf2d427b72bdb2471c22fced7ebd9a7a1)
PASS  Docker engine 23.0.2
PASS  something listens on localhost:3307 (the MySQL of the JDBC labs)
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 3, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `FAIL JDK ... the course needs 17 or newer` | Đang dùng JDK cũ hơn 17 | Cài JDK 17 trở lên và đặt `JAVA_HOME`; chạy lại `java -version` |
| `FAIL Maven: the command mvn was not found` | Maven chưa có trong `PATH` | Cài Maven 3.9 và mở lại terminal; kiểm tra `mvn -v` |
| `WARN Docker is not available` | Docker chưa cài hoặc chưa chạy | Cài Docker và chạy `docker version`; chỉ cần từ chương JDBC trở đi |
