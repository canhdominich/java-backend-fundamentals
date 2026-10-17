# Bài 02 - RowMapper, tham số có tên và batch

Mã nguồn đi kèm bài học **RowMapper, NamedParameter và Batch** (chương 10: Spring JDBC) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: bốn cách biến một dòng thành đối tượng (lambda `RowMapper`, `BeanPropertyRowMapper`, `DataClassRowMapper` vào `record`, `queryForList`), cột NULL với thành phần `int` so với `Integer`, gom kết quả của một `join` bằng `ResultSetExtractor`, `NamedParameterJdbcTemplate` (tham số có tên, danh sách `IN`, một tên dùng hai lần, đầu vào độc hại), `JdbcClient` fluent, `SimpleJdbcInsert` lấy khóa tự sinh, và `batchUpdate` 2000 dòng với và không có `rewriteBatchedStatements` (đo bằng bộ đếm `Com_insert` của MySQL).

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
02-rowmapper-batch/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/java/dev/canhdominich/boot/jdbc/
    MappingApplication.java     @SpringBootApplication
    CourseRecord.java           record có thành phần Integer cho cột có thể NULL
    CourseBean.java             JavaBean cho BeanPropertyRowMapper
    StrictCourse.java           record có thành phần int cho cột có thể NULL (để gây lỗi)
    CatalogDao.java             mọi cách truy cập của bài
    MappingDemo.java            chạy mọi thí nghiệm
  src/test/java/dev/canhdominich/boot/jdbc/
    MySqlLab.java, MappingBehaviourTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec
```

Bước batch mất khoảng nửa phút ở cấu hình mặc định (đó chính là điều được đo).

## Kết quả mong đợi

Kết quả của `MappingDemo` (đã bỏ log ở stderr; số mili giây phụ thuộc máy):

```text
-- four ways to turn a row into an object
lambda RowMapper -> CourseRecord[id=2, title=Spring Data JPA, category=spring, priceVnd=450000, discount=null]
BeanPropertyRowMapper (price_vnd becomes priceVnd) -> priceVnd 450000, title Spring Data JPA
DataClassRowMapper into a record -> CourseRecord[id=2, title=Spring Data JPA, category=spring, priceVnd=450000, discount=null]
queryForList(String.class) -> [Spring Boot, Spring Data JPA, Java Core, JDBC]
-- a nullable column
Integer component, NULL -> null
int component, NULL -> TypeMismatchException: A null value cannot be assigned to a primitive type
-- grouping a join with a ResultSetExtractor
lessonsByCourse -> {Spring Boot=[Starters, Properties], Spring Data JPA=[], Java Core=[Types], JDBC=[]}
-- named parameters
category = :category -> [Java Core, JDBC]
in (:categories) with a list of two -> 4 titles
the same :v used twice, count -> 2
hostile value ' or 1=1 -- -> 0 rows
-- JdbcClient
query(String.class).list() -> [Spring Boot, Spring Data JPA]
query(CourseRecord.class).single() -> CourseRecord[id=2, title=Spring Data JPA, category=spring, priceVnd=450000, discount=null]
optional() for a missing id -> Optional.empty
-- batchUpdate of 2000 rows
default URL: 2000 rows, INSERT commands received by MySQL 2000, counts array: 2000 of 1, 0 of -2 (success, no count), timing 25529 ms
rewriteBatchedStatements=true: 2000 rows, INSERT commands received by MySQL 1, counts array: 0 of 1, 2000 of -2 (success, no count), timing 153 ms
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
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm | `docker compose down` ở lab trước, hoặc `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| Thời gian batch mặc định khác bài | Tốc độ ổ đĩa và độ trễ mạng tới container | So tỉ lệ giữa hai dòng và cột `INSERT commands`, không so mili giây tuyệt đối |
