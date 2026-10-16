# Bài 01 - Giới thiệu JDBC và JDBC Driver

Mã nguồn đi kèm bài học **Giới thiệu JDBC và JDBC Drivers** (chương 2: JDBC) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự thấy: driver được nạp thế nào mà không cần `Class.forName`, `DriverManager` chọn driver ra sao, driver MySQL thật sự gửi gì lên server khi bạn gọi `getConnection`, `PreparedStatement` có phải là câu lệnh biên dịch sẵn hay không, và vì sao một fat jar đóng sai có thể làm mất driver.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Docker + Docker Compose | bất kỳ bản gần đây | MySQL cho demo và Testcontainers cho test |
| MySQL | 8.4 (image `mysql:8.4`) | |
| MySQL Connector/J | 26.7.0 | khai báo trong `pom.xml` |
| H2 | 2.5.252 | driver thứ hai, chỉ để so sánh việc chọn driver |

## Cấu trúc

```text
01-gioi-thieu-jdbc/
  docker-compose.yml           MySQL 8.4, database shop, user app
  pom.xml                      thêm hai profile đóng fat jar: fatjar-broken, fatjar-fixed
  src/main/java/dev/canhdominich/jdbc/intro/
    DbConfig.java              đọc URL, user, mật khẩu từ biến môi trường
    Schema.java                tạo bảng product và hai dòng mẫu
    DriverDiscovery.java       ServiceLoader, DriverManager.getDrivers, getDriver theo URL
    WhatDriverSends.java       bật general_log và in các câu lệnh server nhận được
    ApiTour.java               lớp cài đặt, DatabaseMetaData, NULL, đóng tài nguyên, SQLException
  src/test/java/dev/canhdominich/jdbc/intro/
    DriverDiscoveryTest, ApiBehaviourTest, WhatDriverSendsTest
```

## Chạy MySQL

```bash
docker compose up -d
docker compose ps
```

Chờ `STATUS` hiện `healthy`. MySQL lắng nghe ở cổng 3307 của máy bạn. Nếu bạn đang chạy lab khác dùng cùng cổng, chạy `docker compose down` ở lab đó trước. Biến môi trường mặc định: `JDBC_URL=jdbc:mysql://localhost:3307/shop`, `DB_USER=app`, `DB_PASSWORD=app_pw`, `DB_ROOT_PASSWORD=root_pw`.

## Chạy từng thí nghiệm

Cú pháp chung: `mvn -q compile exec:exec -Dmain.class=<tên lớp>`.

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jdbc.intro.DriverDiscovery
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.intro.ApiTour
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.intro.WhatDriverSends
```

`WhatDriverSends` cần bảng `product`, hãy chạy `ApiTour` trước một lần.

### Thí nghiệm fat jar

```bash
mvn -q -DskipTests -Pfatjar-broken package
java -cp target/lab-broken.jar dev.canhdominich.jdbc.intro.DriverDiscovery

mvn -q -DskipTests -Pfatjar-fixed package
java -cp target/lab-fixed.jar dev.canhdominich.jdbc.intro.DriverDiscovery
```

## Kết quả mong đợi

`DriverDiscovery` khi classpath có cả hai driver:

```text
ServiceLoader finds:
  com.mysql.cj.jdbc.Driver
  org.h2.Driver
getDriver(jdbc:mysql://localhost:3307/shop)  -> com.mysql.cj.jdbc.Driver
getDriver(jdbc:h2:mem:lab)                   -> org.h2.Driver
getDriver(jdbc:postgresql://localhost:5432/shop) -> SQLException SQLState=08001 message=No suitable driver
getDriver(mysql://localhost:3307/shop)           -> SQLException SQLState=08001 message=No suitable driver
```

`lab-broken.jar`: chỉ còn `com.mysql.cj.jdbc.Driver`, và `getDriver(jdbc:h2:mem:lab)` báo `No suitable driver` dù H2 nằm trong jar. `lab-fixed.jar` liệt kê đủ hai driver.

`ApiTour` (trích):

```text
Connection        com.mysql.cj.jdbc.ConnectionImpl
PreparedStatement com.mysql.cj.jdbc.ClientPreparedStatement
price column type DECIMAL -> java.math.BigDecimal
JDBC version   4.2
isolation      4 (REPEATABLE_READ=4)
getInt     = 0, wasNull = true
statement.isClosed = true
class      = java.sql.SQLIntegrityConstraintViolationException
SQLState   = 23000
errorCode  = 1062
SELEC 1 -> SQLException SQLState=S1009 errorCode=0 message=Statement.executeQuery() cannot issue statements that do not produce result sets.
```

`WhatDriverSends`: mỗi lần `getConnection`, server nhận một câu `SELECT @@session.auto_increment_increment ...` đọc cấu hình, rồi `SET character_set_results = NULL`, rồi `SET autocommit=1`. Câu `PreparedStatement` xuất hiện dưới dạng `Query ... WHERE sku = 'KB-01'` (giá trị đã được ghép vào chuỗi), không có lệnh `Prepare` hay `Execute`. Chạy lại với `JDBC_URL='jdbc:mysql://localhost:3307/shop?useServerPrepStmts=true'` thì log hiện `Prepare  SELECT name FROM product WHERE sku = ?`, `Execute`, `Close stmt`.

## Chạy bộ test

Testcontainers tự khởi động MySQL riêng nên cần Docker đang chạy:

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 8, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
docker compose down
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm | `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| `Table 'shop.product' doesn't exist` khi chạy `WhatDriverSends` | Chưa tạo bảng | Chạy `ApiTour` một lần |
| `Access denied for user 'root'` ở `WhatDriverSends` | Mật khẩu root khác mặc định | `export DB_ROOT_PASSWORD=<mật khẩu root>` |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker rồi chạy lại |
