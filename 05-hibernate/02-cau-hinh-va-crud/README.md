# Bài 02 - Cấu hình và CRUD với Hibernate

Mã nguồn đi kèm bài học **Cấu hình và CRUD với Hibernate** (chương 5: Hibernate) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: bốn cách cấu hình Hibernate (bằng mã, tệp thuộc tính, `hibernate.cfg.xml`, `persistence.xml` của JPA) và cách chọn bộ quản lý kết nối (HikariCP hay bộ có sẵn), các chế độ `hbm2ddl.auto` (`none`, `validate`, `update`, `create`, `create-drop`) với kết quả đo được, CRUD qua một lớp `UnitOfWork`, cập nhật hàng loạt bằng HQL làm entity trong bộ nhớ bị cũ, và các bẫy giao dịch (entity detached, quên commit, vi phạm ràng buộc, rollback không hoàn tác bộ nhớ).

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| MySQL | 8.4 (image `mysql:8.4`) |  |
| Hibernate ORM | 7.4.13.Final (kèm `hibernate-hikaricp`) | khai báo trong `pom.xml` |
| HikariCP | 7.1.0 | khai báo trong `pom.xml` |
| MySQL Connector/J | 26.7.0 | khai báo trong `pom.xml` |
| Docker | bất kỳ bản gần đây | MySQL cho demo và Testcontainers cho test |

## Cấu trúc

```text
02-cau-hinh-va-crud/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/resources/
    hibernate.cfg.xml          cấu hình dạng XML
    META-INF/persistence.xml   cấu hình theo chuẩn JPA
    lab/hibernate-lab.properties  cấu hình dạng tệp thuộc tính
  src/main/java/dev/canhdominich/hibernate/crud/
    DbConfig.java              đọc URL, user, mật khẩu từ biến môi trường
    Student.java               entity có cột email duy nhất
    SqlRecorder.java           StatementInspector ghi lại mọi câu SQL
    Configurations.java        bốn cách dựng SessionFactory và hai bộ quản lý kết nối
    UnitOfWork.java            chạy công việc trong một Session và một giao dịch
    ConfigurationDemo.java     so sánh dialect và bộ quản lý kết nối của các cách cấu hình
    SchemaDemo.java            các chế độ hbm2ddl.auto trên bảng thật
    CrudDemo.java              tạo, đọc, sửa, xóa, HQL hàng loạt, dòng không tồn tại
    TransactionPitfallsDemo.java  entity detached, quên commit, ràng buộc, rollback
  src/test/java/dev/canhdominich/hibernate/crud/
    MySqlLab.java, ConfigAndCrudTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.hibernate.crud.ConfigurationDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.crud.SchemaDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.crud.CrudDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.crud.TransactionPitfallsDemo
```

Hibernate ghi log khởi động ra stderr; các dòng bắt đầu bằng `--` hoặc hai dấu cách là kết quả của chương trình. `SchemaDemo` tạo và xóa bảng `student` trong database `shop`.

## Kết quả mong đợi

`ConfigurationDemo`:

```text
programmatic Configuration        dialect=MySQLDialect connectionProvider=HikariCPConnectionProvider maximumPoolSize=10
hibernate.properties-style file    dialect=MySQLDialect connectionProvider=HikariCPConnectionProvider maximumPoolSize=10
hibernate.cfg.xml                  dialect=MySQLDialect connectionProvider=HikariCPConnectionProvider maximumPoolSize=10
persistence.xml (JPA bootstrap)    dialect=MySQLDialect connectionProvider=HikariCPConnectionProvider maximumPoolSize=10
hikaricp module, pool size 7        dialect=MySQLDialect connectionProvider=HikariCPConnectionProvider maximumPoolSize=7
built-in pool forced explicitly     dialect=MySQLDialect connectionProvider=DriverManagerConnectionProvider
```

`SchemaDemo`:

```text
-- no table at all
none           started, columns after: []
validate       FAILED SchemaManagementException: Schema validation: missing table [student]
-- an old table that lacks the column level and has an extra legacy_notes column
validate       FAILED SchemaManagementException: Schema validation: missing column [level] in table [student]
update         started, columns after: [id, full_name, email, legacy_notes, level]
update again   started, columns after: [id, full_name, email, legacy_notes, level]
-- create and create-drop
create         started, columns after: [id, email, full_name, level]
create-drop    started, columns after: []
```

`CrudDemo`:

```text
-- create: persist inside one transaction
  insert into student (email,full_name,level) values (?,?,?)
-- read: Student[1, Nguyễn Văn An, BASIC]
-- SQL of the read
  select s1_0.id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
-- update: load, change, commit
  select s1_0.id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
  update student set email=?,full_name=?,level=? where id=?
-- HQL bulk update changed 1 row; the managed object still says BASIC
-- after refresh() the object says ADVANCED
-- SQL of the bulk update scenario
  select s1_0.id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
  update student s1_0 set level=? where s1_0.id=?
  select s1_0.id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
-- delete: load then remove
  select s1_0.id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
  delete from student where id=?
-- find() of a missing row returns null
-- getReference() of a missing row fails only when used: ObjectNotFoundException
```

`TransactionPitfallsDemo`:

```text
-- persist(detached): EntityExistsException: Detached entity passed to persist: dev.canhdominich.hibernate.crud.Student
-- remove(detached): no exception, rows left = 0
-- a Session closed without commit: rows stored = 0
-- duplicate email, raised by persist() because IDENTITY inserts at once: ConstraintViolationException, cause SQLIntegrityConstraintViolationException
-- transaction status after the failure: MARKED_ROLLBACK, active = true
-- after rollback the object in memory still says: Changed then rolled back
-- and the database says: An
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
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm (ví dụ bởi lab trước chưa tắt) | `docker compose down` ở lab trước, hoặc `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| `SchemaManagementException` khi chạy `CrudDemo` | Bảng `student` còn sót từ `SchemaDemo` với cấu trúc cũ | Chạy `SchemaDemo` đến hết (nó dọn bảng ở cuối) hoặc `DROP TABLE student` trong database `shop` |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker rồi chạy lại |
