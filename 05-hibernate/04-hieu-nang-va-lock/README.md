# Bài 04 - Hiệu năng Hibernate và khóa dữ liệu

Mã nguồn đi kèm bài học **Hiệu năng Hibernate: N+1, batch, projection và khóa** (chương 5: Hibernate) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự đo: số câu SQL của vòng lặp chạm quan hệ lười (N+1) và bốn cách sửa (`join fetch`, `@BatchSize`, entity graph, DTO projection), phân trang khi `join fetch` một collection, chèn hàng loạt với `IDENTITY`, `SEQUENCE` và `rewriteBatchedStatements`, mất cập nhật khi đọc rồi ghi, khóa lạc quan với `@Version` và khóa bi quan `PESSIMISTIC_WRITE`.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| MySQL | 8.4 (image `mysql:8.4`) |  |
| Hibernate ORM | 7.4.13.Final | khai báo trong `pom.xml` |
| MySQL Connector/J | 26.7.0 | khai báo trong `pom.xml` |
| Docker | bất kỳ bản gần đây | MySQL cho demo và Testcontainers cho test |

## Cấu trúc

```text
04-hieu-nang-va-lock/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/java/dev/canhdominich/hibernate/performance/
    DbConfig.java, SqlRecorder.java, UnitOfWork.java   như bài trước
    Teacher.java, Course.java, Lesson.java, Review.java  mô hình khóa học (Course có @Version và @BatchSize)
    CourseSummary.java         record dùng cho DTO projection
    MetricIdentity.java, MetricSequence.java            hai entity chèn hàng loạt, khác cách sinh khóa
    Factories.java             dựng SessionFactory, nhận thêm thuộc tính cấu hình
    SampleData.java            20 khóa học, mỗi khóa 5 bài học và 3 đánh giá
    NPlusOneDemo.java          N+1 và các cách sửa, phân trang
    BatchInsertDemo.java       đo chèn hàng loạt bằng bộ đếm Com_insert của MySQL
    LockingDemo.java           mất cập nhật, @Version, PESSIMISTIC_WRITE
  src/test/java/dev/canhdominich/hibernate/performance/
    MySqlLab.java, PerformanceBehaviourTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.hibernate.performance.NPlusOneDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.performance.BatchInsertDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.performance.LockingDemo
```

Hibernate ghi log khởi động ra stderr. Lần đầu bạn sẽ thấy vài cảnh báo `Table 'shop.course' doesn't exist` khi `create-drop` cố xóa bảng chưa có: vô hại. Các dòng kết quả của chương trình bắt đầu bằng chữ cái hoa, `--` hoặc hai dấu cách.

## Kết quả mong đợi

`NPlusOneDemo` (số câu SQL mỗi thí nghiệm, 20 khóa học, 4 giáo viên):

```text
A. lazy collection in a loop -> 21 statements
B. join fetch -> 1 statements
C. @BatchSize(size = 10) on reviews -> 3 statements
D. entity graph -> 1 statements
E. DTO projection -> 1 statements
F. lazy many-to-one in a loop -> 5 statements
G. join fetch on many-to-one -> 1 statements
H. pagination with a collection fetch -> 1 statements
I. pagination in two steps -> 2 statements
```

SQL của thí nghiệm H: Hibernate đặt `limit` vào truy vấn con nên chỉ 5 khóa học đầu được nối với bài học:

```text
select c1_0.id,l1_0.course_id,l1_0.id,l1_0.title,c1_0.seats_left,c1_0.teacher_id,c1_0.title,c1_0.version from (select distinct c1_0.id,c1_0.version,c1_0.seats_left,c1_0.teacher_id,c1_0.title from course c1_0 where exists(select 1 from lesson l1_0 where c1_0.id=l1_0.course_id) order by c1_0.id limit ?,?) c1_0(id,version,seats_left,teacher_id,title) join lesson l1_0 on c1_0.id=l1_0.course_id order by c1_0.id
```

`BatchInsertDemo` (5000 dòng; thời gian phụ thuộc máy, cột `Com_insert` thì ổn định):

```text
IDENTITY, batch_size=50                                      6376 ms    5000 Com_insert
SEQUENCE, batch_size=50                                      4634 ms    5000 Com_insert
SEQUENCE, batch_size=50, rewriteBatchedStatements            1922 ms     100 Com_insert
```

`LockingDemo`:

```text
read-modify-write twice: seatsLeft=29 (expected 28)
atomic UPDATE twice: seatsLeft=28
both read: seatsLeft=28 version=0
first commit ok
second commit failed: jakarta.persistence.OptimisticLockException / jakarta.persistence.OptimisticLockException
second transaction waited 1517 ms for the row lock
pessimistic find -> 2 statements
  select c1_0.id,c1_0.seats_left,c1_0.teacher_id,c1_0.title,c1_0.version from course c1_0 where c1_0.id=? for update of c1_0
  update course set seats_left=?,teacher_id=?,title=?,version=? where id=? and version=?
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 6, Failures: 0, Errors: 0` (khoảng một phút vì Testcontainers khởi động MySQL và bài đo chèn hàng loạt).

## Dọn dẹp

```bash
mvn clean
docker compose down
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm (ví dụ bởi lab trước chưa tắt) | `docker compose down` ở lab trước, hoặc `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| Thời gian chèn khác số trong README | Tốc độ ổ đĩa và CPU | So sánh tỉ lệ và cột `Com_insert`, không so mili giây tuyệt đối |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker rồi chạy lại |
