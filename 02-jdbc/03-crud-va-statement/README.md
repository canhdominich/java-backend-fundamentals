# Bài 03 - CRUD và các loại Statement

Mã nguồn đi kèm bài học **CRUD và các loại Statement** (chương 2: JDBC) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: bốn thao tác CRUD với `PreparedStatement`, SQL injection thật trên MySQL (kể cả đòn xếp chồng lệnh), so sánh ba chế độ chuẩn bị câu lệnh bằng thời gian, giá trị trả về của `executeUpdate`, bẫy `ORDER BY ?`, đọc 400.000 dòng với heap 64 MB, và gọi stored procedure bằng `CallableStatement`.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Docker + Docker Compose | bất kỳ bản gần đây | MySQL cho demo và Testcontainers cho test |
| MySQL | 8.4 (image `mysql:8.4`) | |
| MySQL Connector/J | 26.7.0 | khai báo trong `pom.xml` |

## Cấu trúc

```text
03-crud-va-statement/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml                      thuộc tính main.class, main.arg, jvm.heap cho exec:exec
  src/main/java/dev/canhdominich/jdbc/crud/
    DbConfig, Db               cấu hình kết nối, mở kết nối, tạo schema
    Product.java               record ánh xạ một dòng bảng product
    CrudDemo.java              insert, findById, search, update, delete
    SqlInjectionDemo.java      đăng nhập nối chuỗi và đăng nhập PreparedStatement; xếp chồng lệnh
    PreparedModes.java         client-side, server-side, server-side có cache: 5000 truy vấn
    AffectedRows.java          giá trị executeUpdate trả về với và không có useAffectedRows
    OrderByAndIn.java          ORDER BY ?, whitelist cột sắp xếp, danh sách IN
    LargeResult.java           đọc 400.000 dòng: default, stream, cursor, và bị chặn khi đang stream
    StoredProcedureDemo.java   CallableStatement với tham số IN và OUT
  src/test/java/dev/canhdominich/jdbc/crud/
    CrudAndStatementsTest.java 8 test
```

## Chạy MySQL

```bash
docker compose up -d
docker compose ps
```

Chờ `STATUS` là `healthy`. MySQL ở cổng 3307. Nếu bạn đang chạy lab khác dùng cùng cổng, chạy `docker compose down` ở lab đó trước. Biến môi trường mặc định: `JDBC_URL=jdbc:mysql://localhost:3307/shop`, `DB_USER=app`, `DB_PASSWORD=app_pw`.

## Chạy từng thí nghiệm

Cú pháp chung: `mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.<Lớp>`.

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.CrudDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.SqlInjectionDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.PreparedModes
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.AffectedRows
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.OrderByAndIn
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.StoredProcedureDemo
```

`LargeResult` nhận chế độ qua `-Dmain.arg` và giới hạn heap qua `-Djvm.heap`:

```bash
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.LargeResult -Dmain.arg=seed
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.LargeResult -Dmain.arg=default -Djvm.heap=-Xmx64m
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.LargeResult -Dmain.arg=stream -Djvm.heap=-Xmx64m
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.LargeResult -Dmain.arg=cursor -Djvm.heap=-Xmx64m
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.crud.LargeResult -Dmain.arg=blocked
```

Bảng `big_table` do chế độ `seed` tạo, khoảng 400.000 dòng mỗi dòng 200 ký tự, chiếm vài chục MB trong volume của container.

## Kết quả mong đợi

Thời gian phụ thuộc máy bạn; quan hệ giữa các con số mới là điều cần nhìn.

`SqlInjectionDemo`:

```text
login(an | hash-an)  concat=LOGGED_IN as USER  prepared=LOGGED_IN as USER
login(an | wrong)  concat=denied  prepared=denied
login(admin' --  | anything)  concat=LOGGED_IN as ADMIN  prepared=denied
login(nobody | x' OR '1'='1)  concat=LOGGED_IN as ADMIN  prepared=denied
stacked query with default URL: rejected SQLSyntaxErrorException SQLState=42000, audit_log exists = true
stacked query with allowMultiQueries=true: query ran, audit_log exists = false
```

`PreparedModes` (5000 lần mở, chạy, đóng một `PreparedStatement`):

```text
default (client-side)                            5000 queries in ~3000 ms
useServerPrepStmts=true                          5000 queries in ~3500-4000 ms
useServerPrepStmts=true&cachePrepStmts=true      5000 queries in ~1500 ms
```

`AffectedRows`:

```text
default: same value -> 1, new value -> 1, no such row -> 0
useAffectedRows=true: same value -> 0, new value -> 1, no such row -> 0
```

`OrderByAndIn`: `ORDER BY ?` giữ nguyên thứ tự chèn `[Charger, Adapter, Battery]`, còn `ORDER BY price` cho `[Adapter, Battery, Charger]`.

`LargeResult` với `-Xmx64m` và 400.000 dòng:

```text
default: SQLException message="Java heap space" cause=OutOfMemoryError
stream: rows=400000 chars=80000000 time=~2200 ms
cursor: rows=400000 chars=80000000 time=~3600-4100 ms
```

Chế độ `blocked` in `SQLState=S1000` với thông điệp `Streaming result set ... is still active. No statements may be issued when any streaming result sets are open ...`.

`StoredProcedureDemo`: `count_products(15.00) = 1`, `count_products(5.00)  = 2`.

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 8, Failures: 0, Errors: 0`. Test tự khởi động MySQL riêng bằng Testcontainers nên cần Docker đang chạy.

## Dọn dẹp

```bash
docker compose down -v
mvn clean
```

Cờ `-v` xóa luôn volume dữ liệu, kể cả bảng `big_table`.

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm | `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| `Table 'shop.big_table' doesn't exist` | Chưa tạo dữ liệu mẫu | Chạy `LargeResult` với `-Dmain.arg=seed` |
| `LargeResult default` không lỗi bộ nhớ | Heap lớn hơn dữ liệu | Giữ `-Djvm.heap=-Xmx64m` |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker rồi chạy lại |
