# Bài 01 - Giới thiệu và kiến trúc Hibernate

Mã nguồn đi kèm bài học **Giới thiệu và kiến trúc Hibernate** (chương 5: Hibernate) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy Hibernate 7 trên MySQL và nhìn từng câu SQL mà nó sinh ra: chi phí tạo `SessionFactory` so với mở `Session`, thời điểm `INSERT` với khóa `IDENTITY` và với `SEQUENCE`, bộ nhớ đệm cấp một (persistence context), dirty checking, ba trạng thái của entity và `merge`, tự động flush trước truy vấn, proxy của `getReference`, và `@DynamicUpdate`. Một lớp `SqlRecorder` ghi lại mọi câu SQL qua `StatementInspector` để bài học có thể trích dẫn chính xác.

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
01-gioi-thieu-kien-truc/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/java/dev/canhdominich/hibernate/intro/
    DbConfig.java              đọc URL, user, mật khẩu từ biến môi trường
    Student.java               entity chính, khóa IDENTITY, enum lưu dạng chuỗi
    Ticket.java                entity dùng khóa SEQUENCE (allocationSize 50)
    Note.java                  entity có @DynamicUpdate
    SqlRecorder.java           StatementInspector ghi lại mọi câu SQL
    Factories.java             dựng SessionFactory bằng mã, create-drop, bật thống kê
    PersistenceContextDemo.java  thí nghiệm về Session, persistence context, trạng thái entity
    IdStrategiesDemo.java      IDENTITY, SEQUENCE và @DynamicUpdate
  src/test/java/dev/canhdominich/hibernate/intro/
    MySqlLab.java, HibernateIntroTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.hibernate.intro.PersistenceContextDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.intro.IdStrategiesDemo
```

Hibernate ghi log khởi động ra stderr (phiên bản, dialect, thông tin kết nối); các dòng bắt đầu bằng `--` hoặc hai dấu cách là kết quả của chương trình. Các bảng do Hibernate tạo ở lúc khởi động và xóa lúc đóng `SessionFactory` (`hbm2ddl.auto=create-drop`), nên chạy lặp lại không cần dọn dẹp.

## Kết quả mong đợi

```text
-- building the SessionFactory took more than 20 times longer than opening a Session: true
-- persist: state before = transient, id = null
-- SQL right after persist(), before commit (IDENTITY must know the id)
  insert into student (email,full_name,level) values (?,?,?)
-- state after persist: managed = true, id = 1
-- SQL at commit
  (no SQL)
-- two find() calls in one Session: same object = true
-- SQL for two find() calls
  select s1_0.id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
-- SQL for a find() after clear()
  select s1_0.id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
-- SQL after setFullName(), before commit
  (no SQL)
-- SQL at commit (no update() call was made)
  update student set email=?,full_name=?,level=? where id=?
-- SQL when the value is set to what it already was
  (no SQL)
-- after the Session closed: still usable as an object = Nguyễn Văn An (đã sửa)
-- merge() returns a different object: true, the argument is managed: false, the result is managed: true
-- SQL for merge() of a detached object
  select s1_0.id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
  update student set email=?,full_name=?,level=? where id=?
-- a query after an in-memory change counts the change: 1
-- SQL: the pending UPDATE is flushed before the query
  update student set email=?,full_name=?,level=? where id=?
  select count(s1_0.id) from student s1_0 where s1_0.level=?
-- getReference(): initialized = false, class = Student subclass
-- SQL after getReference()
  (no SQL)
-- reading the id does not load it, reading a field does: id = 1
-- SQL after getId()
  (no SQL)
-- name = Nguyễn Văn An (đã sửa)
-- SQL after getFullName()
  select s1_0.id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
```

`IdStrategiesDemo`:

```text
-- IDENTITY: three persist() calls, nothing committed yet
  insert into student (email,full_name,level) values (?,?,?)
  insert into student (email,full_name,level) values (?,?,?)
  insert into student (email,full_name,level) values (?,?,?)
-- SEQUENCE (allocationSize 50): three persist() calls, nothing committed yet
  (no SQL)
-- JDBC statements prepared during those three persist() calls: 4 (reading the id generator)
-- MySQL has no sequences, Hibernate keeps the counter in a table: [ticket_seq]
-- SEQUENCE: SQL at commit
  insert into Ticket (subject,id) values (?,?)
  insert into Ticket (subject,id) values (?,?)
  insert into Ticket (subject,id) values (?,?)
-- @DynamicUpdate: only the changed column is in the UPDATE
  update Note set title=? where id=?
```

Dòng `JDBC statements prepared ... : 4` là số lần Hibernate đọc và cập nhật bảng đếm `ticket_seq` để cấp mã; số này có thể khác một chút giữa các lần chạy đầu và các lần sau.

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
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm (ví dụ bởi lab trước chưa tắt) | `docker compose down` ở lab trước, hoặc `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| `Communications link failure` khi chạy chương trình | MySQL chưa sẵn sàng hoặc sai cổng | `docker compose ps` chờ `healthy`, kiểm tra `JDBC_URL` |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker rồi chạy lại |
