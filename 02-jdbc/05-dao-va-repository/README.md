# Bài 05 - DAO và Repository thuần JDBC

Mã nguồn đi kèm bài học **DAO và Repository thuần JDBC** (chương 2: JDBC) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một lớp Repository dùng HikariCP, dịch `SQLException` sang ngoại lệ của ứng dụng, một transaction bao nhiều Repository bằng `ThreadLocal` (cùng ý tưởng với `@Transactional` của Spring), chứng minh vì sao thiếu nó thì dữ liệu nửa vời, đếm câu lệnh N+1, so sánh phân trang OFFSET với keyset trên 500.000 dòng, và kiểm thử logic nghiệp vụ bằng Repository giả không cần cơ sở dữ liệu.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Docker + Docker Compose | bất kỳ bản gần đây | MySQL cho demo và Testcontainers cho test |
| MySQL | 8.4 (image `mysql:8.4`) | |
| MySQL Connector/J | 26.7.0 | khai báo trong `pom.xml` |
| HikariCP | 7.1.0 | khai báo trong `pom.xml` |

## Cấu trúc

```text
05-dao-va-repository/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/java/dev/canhdominich/jdbc/dao/
    DbConfig, DataSources, Schema   cấu hình, HikariCP, tạo bảng và dữ liệu mẫu
    Product, ProductRepository      interface và record của sản phẩm
    JdbcProductRepository           cài đặt JDBC của ProductRepository
    OrderRepository                 đơn hàng, N+1 và JOIN
    OrderService                    nghiệp vụ đặt hàng: trừ kho, tạo đơn, tạo dòng đơn
    TransactionContext              ThreadLocal giữ kết nối của transaction hiện tại
    DataAccessException             ngoại lệ không kiểm tra, DuplicateKeyException
    LowStockReport                  logic chỉ phụ thuộc interface, kiểm thử không cần DB
    QueryCounter                    đếm câu lệnh máy chủ nhận được
    PartialFailureDemo, NPlusOneDemo, PaginationDemo   ba thí nghiệm
  src/test/java/dev/canhdominich/jdbc/dao/
    RepositoryTest.java             9 test
```

## Chạy MySQL

```bash
docker compose up -d
docker compose ps
```

Chờ `STATUS` là `healthy`. MySQL ở cổng 3307. Nếu bạn đang chạy lab khác dùng cùng cổng, chạy `docker compose down` ở lab đó trước. Biến môi trường mặc định: `JDBC_URL=jdbc:mysql://localhost:3307/shop`, `DB_USER=app`, `DB_PASSWORD=app_pw`.

## Chạy từng thí nghiệm

Cú pháp chung: `mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jdbc.dao.<Lớp>`.

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jdbc.dao.PartialFailureDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.dao.NPlusOneDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.dao.PaginationDemo
```

`PaginationDemo` tạo 500.000 dòng và mất khoảng 20 giây.

## Kết quả mong đợi

Thời gian phụ thuộc máy bạn; quan hệ giữa các con số mới là điều cần nhìn.

`PartialFailureDemo` (đặt hàng với email NULL để bước thứ hai thất bại):

```text
no transaction  -> DataAccessException(insert order) cause=SQLIntegrityConstraintViolationException, stock of KB-01 10 -> 7
with transaction -> DataAccessException(insert order) cause=SQLIntegrityConstraintViolationException, stock of KB-01 7 -> 7
```

`NPlusOneDemo` (200 đơn, mỗi đơn 2 dòng):

```text
one query per order : 200 orders, about 201 statements
single JOIN         : 200 orders, about 1 statements
same result         : true
```

`PaginationDemo` (20 dòng mỗi trang, thời gian tốt nhất trong 5 lần):

```text
page starting at row 0        OFFSET: ~4ms    keyset: ~2ms
page starting at row 100000   OFFSET: ~44ms   keyset: ~2ms
page starting at row 499000   OFFSET: ~220ms  keyset: ~2ms
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 9, Failures: 0, Errors: 0`. Test tự khởi động MySQL riêng bằng Testcontainers nên cần Docker đang chạy.

## Dọn dẹp

```bash
docker compose down -v
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm | `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| `PaginationDemo` chạy rất lâu ở bước tạo dữ liệu | Ổ đĩa chậm | Giảm `ROWS` trong `PaginationDemo` |
| Số câu lệnh N+1 lệch vài đơn vị | Có kết nối khác đang gửi lệnh tới cùng MySQL | Dừng các ứng dụng khác dùng cổng 3307 |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker rồi chạy lại |
