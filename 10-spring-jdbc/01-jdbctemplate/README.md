# Bài 01 - Spring JDBC và JdbcTemplate

Mã nguồn đi kèm bài học **Spring JDBC và JdbcTemplate** (chương 10: Spring JDBC) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: `JdbcTemplate` cho các thao tác ghi và đọc (khóa tự sinh bằng `KeyHolder`, số dòng ảnh hưởng của `update`), kết nối được mượn và trả về pool mà bạn không phải viết `close`, các ngoại lệ được dịch thành loại có nghĩa (`EmptyResultDataAccessException`, `IncorrectResultSizeDataAccessException`, `DuplicateKeyException`, `BadSqlGrammarException`, `QueryTimeoutException`), và `@Transactional` cho JDBC thuần: không có nó, lệnh chèn đầu tiên vẫn được lưu khi có lỗi.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml` |
| MySQL | 8.4 (image `mysql:8.4`) | cổng 3307 của máy bạn |
| Docker | bất kỳ bản gần đây | MySQL cho demo và Testcontainers cho test |

## Cấu trúc

```text
01-jdbctemplate/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml                       parent Boot 4.1.1, starter jdbc, mysql-connector-j, test, testcontainers
  src/main/java/dev/canhdominich/boot/jdbc/
    JdbcApplication.java        @SpringBootApplication
    Student.java                record sinh viên
    StudentDao.java             truy cập dữ liệu bằng JdbcTemplate
    RegistrationService.java    hai phương thức: có và không có @Transactional
    JdbcTemplateDemo.java       chạy mọi thí nghiệm
  src/test/java/dev/canhdominich/boot/jdbc/
    MySqlLab.java, JdbcTemplateBehaviourTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `JdbcTemplateDemo` (đã bỏ log ở stderr):

```text
-- writing and reading
generated keys: 1, 2
findById(1) -> Student[id=1, name=An, email=an@example.com]
findAll -> 2 students
rename existing id -> rows updated 1
rename missing id -> rows updated 0
count -> 2
-- connections: borrowed and returned for you
active connections after the calls above: 0
-- exceptions with a clear meaning
queryForObject, no row -> EmptyResultDataAccessException (root cause EmptyResultDataAccessException): Incorrect result size: expected 1, actual 0
findByEmail wrapper for the same case -> Optional.empty
queryForObject, two rows -> IncorrectResultSizeDataAccessException (root cause IncorrectResultSizeDataAccessException): Incorrect result size: expected 1, actual 2
duplicate email -> DuplicateKeyException (root cause SQLIntegrityConstraintViolationException): PreparedStatementCallback; Duplicate entry 'an@example.com' for key 'student.email'
bad SQL -> BadSqlGrammarException (root cause SQLSyntaxErrorException): StatementCallback; bad SQL grammar [select nope from student]
query timeout of 1 s on select sleep(3) -> QueryTimeoutException (root cause MySQLTimeoutException): StatementCallback; SQL [select sleep(3)]; Statement cancelled due to timeout or client request
-- transactions
without @Transactional, failure after one insert: IllegalStateException (root cause IllegalStateException): failure after the first insert; rows added 1
with @Transactional, same failure: IllegalStateException (root cause IllegalStateException): failure after the first insert; rows added 0
active connections at the end: 0
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
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm | `docker compose down` ở lab trước, hoặc `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| Dòng `query timeout` không thấy `QueryTimeoutException` | Máy chủ trả kết quả nhanh hơn 1 giây | `select sleep(3)` luôn chậm hơn 1 giây trên MySQL; kiểm tra bạn dùng đúng MySQL |
