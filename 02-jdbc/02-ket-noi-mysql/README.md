# Bài 02 - Kết nối MySQL bằng JDBC

Mã nguồn đi kèm bài học **Kết nối MySQL** (chương 2: JDBC) của khóa *Java Backend: JDBC, Servlet, Hibernate và Spring*.

Bạn sẽ chạy lại các thí nghiệm trong bài để tự thấy kết quả: kết nối bằng `DriverManager`, đọc từng loại lỗi kết nối, so sánh mở kết nối trực tiếp với connection pool (HikariCP), làm cạn pool, và thấy vì sao múi giờ có thể làm lệch dữ liệu.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Docker + Docker Compose | bất kỳ bản gần đây | Chạy MySQL cho các demo và cho bộ test |
| MySQL | 8.4 (image `mysql:8.4`) | Docker tự tải ở lần chạy đầu |
| MySQL Connector/J | 26.7.0 | Khai báo trong `pom.xml` |
| HikariCP | 7.1.0 | Khai báo trong `pom.xml` |

## Cấu trúc

```text
02-ket-noi-mysql/
  docker-compose.yml          MySQL 8.4, database shop, user app
  pom.xml
  src/main/java/dev/canhdominich/jdbc/
    DbConfig.java             đọc URL, user, mật khẩu từ biến môi trường
    HelloConnection.java      kết nối tối thiểu và in thông tin phiên
    ConnectionErrors.java     tái hiện 4 lỗi kết nối thường gặp
    PoolVsDirect.java         so sánh DriverManager và HikariCP
    PoolExhaustion.java       cạn pool và cảnh báo rò rỉ kết nối
    TimezoneDemo.java         cùng một thời điểm, lưu lệch 7 giờ hay đúng
  src/test/java/dev/canhdominich/jdbc/
    ConnectionErrorsTest.java mã lỗi và SQLState của từng tình huống
    PoolBehaviourTest.java    tái dùng kết nối và hết thời gian chờ pool
    TimezoneAndLimitsTest.java múi giờ, tham số URL, giới hạn max_connections
```

## Chạy MySQL

```bash
docker compose up -d
docker compose ps
```

Chờ cột `STATUS` hiện `healthy`. MySQL lắng nghe ở cổng **3307** của máy bạn (không phải 3306) để không đụng MySQL đã cài sẵn. Muốn đổi cổng, đặt biến `MYSQL_PORT` khi chạy compose và đặt `JDBC_URL` tương ứng.

Thông tin kết nối mặc định (khớp `docker-compose.yml`):

| Biến môi trường | Giá trị mặc định |
|---|---|
| `JDBC_URL` | `jdbc:mysql://localhost:3307/shop` |
| `DB_USER` | `app` |
| `DB_PASSWORD` | `app_pw` |

## Chạy từng thí nghiệm

Cú pháp chung: `mvn -q compile exec:exec -Dmain.class=<tên lớp>`.

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jdbc.HelloConnection
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.ConnectionErrors
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.PoolVsDirect
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.PoolExhaustion
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.TimezoneDemo
```

## Kết quả mong đợi

Số liệu thời gian phụ thuộc máy bạn; quan hệ giữa các con số mới là điều cần nhìn.

`HelloConnection`:

```text
Connected to MySQL 8.4.9
Driver: MySQL Connector/J mysql-connector-j-26.7.0 (Revision: ...)
Catalog: shop
isValid(2): true
Server connection id: <một số nguyên>
Authenticated as: app@%
```

`ConnectionErrors` (bốn khối, mỗi khối một tình huống):

| Tình huống | Lớp ngoại lệ | SQLState | errorCode |
|---|---|---|---|
| Sai mật khẩu | `SQLException` | 28000 | 1045 |
| Sai cổng | `CommunicationsException` | 08S01 | 0 (gốc là `Connection refused`) |
| Sai tên database (user thường) | `SQLSyntaxErrorException` | 42000 | 1044 |
| Sai tiền tố URL | `SQLException` | 08001 | 0 (`No suitable driver found`) |

`PoolVsDirect`: 200 lần mượn/trả. `DriverManager` tạo **200** kết nối phía server, `HikariCP` chỉ **1** và nhanh hơn rõ rệt (trong lần đo của bài: 5056 ms so với 265 ms).

`PoolExhaustion`: pool tối đa 2 kết nối, lần mượn thứ ba thất bại sau khoảng 1 giây với `SQLTransientConnectionException: ... Connection is not available, request timed out after 1000ms (total=2, active=2, idle=0, waiting=0)`, và log có hai cảnh báo `Connection leak detection triggered`.

`TimezoneDemo` (khi JVM chạy ở `Asia/Ho_Chi_Minh`, server ở UTC):

```text
default                  server shows 2026-10-16 10:00:00, epoch 1792144800, offset from truth +7 h
connectionTimeZone=UTC   server shows 2026-10-16 03:00:00, epoch 1792119600, offset from truth +0 h
```

## Chạy bộ test

Bộ test tự khởi động MySQL riêng bằng Testcontainers nên cần Docker đang chạy, không cần `docker compose up`:

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 13, Failures: 0, Errors: 0`. Lần đầu có thể mất 30 giây trở lên vì tải image.

## Dọn dẹp

```bash
docker compose down
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm | `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| `Access denied for user 'app'` ngay lần chạy đầu | Đang kết nối nhầm vào một MySQL khác trên cùng cổng | Kiểm tra `JDBC_URL`, dùng cổng riêng của lab |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker Desktop hoặc `sudo systemctl start docker` |
