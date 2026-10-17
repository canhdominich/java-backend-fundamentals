# Bài 02 - Truy vấn, phân trang và chiếu dữ liệu

Mã nguồn đi kèm bài học **Query Methods, Pagination và Sorting** (chương 9: Spring Data JPA) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: truy vấn dẫn xuất từ tên phương thức (`Containing`, `Between`, `In`, `Top3`, `count`, `exists`) và SQL chúng sinh ra, `@Query` bằng JPQL và SQL gốc, ba kiểu trả về của phân trang (`Page` gửi thêm câu đếm, `Slice` và `List` thì không, và `Page` bỏ câu đếm khi trang đầu chưa đầy), sắp xếp và lỗi khi sắp xếp theo thuộc tính không có, chiếu dữ liệu (entity, interface, `record`, động), cập nhật hàng loạt bằng `@Modifying` làm thực thể đã nạp bị cũ, và lỗi gõ sai tên thuộc tính trong tên phương thức dừng ứng dụng lúc khởi động.

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
02-truy-van-phan-trang/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/java/dev/canhdominich/boot/query/
    QueryApplication.java       @SpringBootApplication
    Level.java, Course.java     enum và entity khóa học
    CourseTitle.java            projection dạng interface
    CourseSummary.java          projection dạng record
    CourseRepository.java       toàn bộ truy vấn của bài
    SqlRecorder.java            StatementInspector ghi lại SQL
    QueryDemo.java              chạy mọi thí nghiệm và in SQL
  src/main/java/dev/canhdominich/boot/broken/
    BrokenRepository.java       repository có tên phương thức sai chính tả
    BrokenApplication.java      cấu hình riêng để khởi động nó và thấy lỗi
  src/test/java/dev/canhdominich/boot/query/
    MySqlLab.java, QueryBehaviourTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `QueryDemo` (đã bỏ log ở stderr):

```text
-- query methods derived from the name
findByTitleContainingIgnoreCase("course 1") -> 11 rows; 1 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where upper(c1_0.title) like upper(?) escape '\\'
findByPriceVndBetween(150000, 200000) -> 6 rows; 1 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.price_vnd between ? and ?
findByLevelInOrderByPriceVndDesc(BASIC, ADVANCED) -> Course 30 first; 1 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.level in (?,?) order by c1_0.price_vnd desc
findTop3ByOrderByPriceVndAsc() -> 3 rows; 1 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 order by c1_0.price_vnd limit ?
countByLevel(ADVANCED) -> 10 rows; 1 statements
    select count(c1_0.id) from course c1_0 where c1_0.level=?
existsByTitle("Course 5") -> true; 1 statements
    select c1_0.id from course c1_0 where c1_0.title=? limit ?
-- @Query
JPQL expensiveIn("java", 350000) -> 3 rows; 1 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.category=? and c1_0.price_vnd>=?
native cheapestTwoNative("java") -> 2 rows; 1 statements
    select * from course where category = ? order by price_vnd limit 2
-- pagination: what each return type costs
Page: findByLevel(BASIC, page 1, size 5) -> content 5, total 20, pages 4, number 1; 2 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.level=? limit ?,?
    select count(c1_0.id) from course c1_0 where c1_0.level=?
Slice: findSliceByLevel(BASIC, page 1, size 5) -> content 5, hasNext true; 1 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.level=? limit ?,?
List: findListByLevel(BASIC, page 1, size 5) -> 5 rows; 1 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.level=? limit ?,?
Page with size 100 over 20 matching rows -> content 20, total 20; 1 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.level=? limit ?
-- sorting
page 0, size 3, sort by priceVnd descending -> Course 29 first; 2 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.level=? order by c1_0.price_vnd desc limit ?
    select count(c1_0.id) from course c1_0 where c1_0.level=?
sort by an unknown property -> PropertyReferenceException: PropertyReferenceException: No property 'nope' found for type 'Course'
-- projections
entity: findListByLevel(BASIC, first 1) -> 1 row; 1 statements
    select c1_0.id,c1_0.category,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.level=? limit ?
interface projection findTitlesByLevel -> Course 1; 1 statements
    select c1_0.title from course c1_0 where c1_0.level=?
record projection findSummariesByLevel -> CourseSummary[title=Course 1, priceVnd=110000]; 1 statements
    select c1_0.title,c1_0.price_vnd from course c1_0 where c1_0.level=?
dynamic projection findByCategory("java", CourseTitle.class) -> 15 rows; 1 statements
    select c1_0.title from course c1_0 where c1_0.category=?
-- bulk update through @Query
stale entity after a bulk update inside one transaction: price before 130000, rows updated 10, price read again 130000
stale entity with clearAutomatically = true: price before 130001, rows updated 10, price read again 130002
-- a misspelled property is caught at startup
startup failed: BeanCreationException / PropertyReferenceException: No property 'titel' found for type 'Course'; Did you mean 'title'
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 7, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm | `docker compose down` ở lab trước, hoặc `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| Dòng `stale entity` cho giá khác bài | Giá cộng dồn qua các lần chạy trên cùng một bảng | Mỗi lần chạy bảng được tạo lại (`create-drop`); chạy đúng lệnh trong README |
