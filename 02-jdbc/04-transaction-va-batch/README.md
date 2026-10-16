# Bài 04 - Transaction và Batch

Mã nguồn đi kèm bài học **Transaction và Batch** (chương 2: JDBC) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: chuyển tiền có commit và rollback, mất tiền khi thiếu transaction, savepoint, ba mức cô lập quan sát từ hai kết nối, lost update và ba cách chặn nó, deadlock thật với mã lỗi 1213, hết thời gian chờ khóa 1205, hàm thử lại, và đo batch với `rewriteBatchedStatements`.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Docker + Docker Compose | bất kỳ bản gần đây | MySQL cho demo và Testcontainers cho test |
| MySQL | 8.4 (image `mysql:8.4`), InnoDB | CHECK constraint được thực thi từ MySQL 8.0.16 |
| MySQL Connector/J | 26.7.0 | khai báo trong `pom.xml` |

## Cấu trúc

```text
04-transaction-va-batch/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/java/dev/canhdominich/jdbc/tx/
    DbConfig, Db               cấu hình, mở kết nối, tạo bảng account
    TransferDemo.java          chuyển tiền, rollback, mất tiền khi autocommit, savepoint
    IsolationDemo.java         READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ từ hai kết nối
    LostUpdateDemo.java        đọc rồi ghi, UPDATE nguyên tử, FOR UPDATE, cột version
    DeadlockDemo.java          deadlock 1213, hết thời gian chờ khóa 1205, hàm thử lại
    TxRunner.java              chạy công việc trong transaction, thử lại khi SQLState 40001
    BatchBenchmark.java        bốn cách chèn hàng loạt
    BatchErrors.java           mảng kết quả khi một lệnh trong lô lỗi
  src/test/java/dev/canhdominich/jdbc/tx/
    TransactionBehaviourTest.java   11 test
```

## Chạy MySQL

```bash
docker compose up -d
docker compose ps
```

Chờ `STATUS` là `healthy`. MySQL ở cổng 3307. Nếu bạn đang chạy lab khác dùng cùng cổng, chạy `docker compose down` ở lab đó trước. Biến môi trường mặc định: `JDBC_URL=jdbc:mysql://localhost:3307/shop`, `DB_USER=app`, `DB_PASSWORD=app_pw`.

## Chạy từng thí nghiệm

Cú pháp chung: `mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jdbc.tx.<Lớp>`.

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.jdbc.tx.TransferDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.tx.IsolationDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.tx.LostUpdateDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.tx.DeadlockDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.tx.BatchErrors
mvn -q exec:exec -Dmain.class=dev.canhdominich.jdbc.tx.BatchBenchmark
```

`BatchBenchmark` chèn 2000 dòng mỗi cách. Cách autocommit từng dòng mất vài chục giây vì mỗi lệnh là một lần commit ghi xuống đĩa, hãy kiên nhẫn.

## Kết quả mong đợi

Thời gian phụ thuộc máy bạn, đặc biệt là tốc độ ghi đĩa; quan hệ giữa các con số mới là điều cần nhìn.

`TransferDemo`:

```text
start                         : An=100.00 Binh=50.00
transfer 30 An->Binh          : An=70.00 Binh=80.00
transfer 500 failed           : SQLException SQLState=HY000 errorCode=3819
after the failed transfer     : An=70.00 Binh=80.00
autocommit, failure in step 2 : An=70.00 Binh=60.00
```

Dòng cuối: bước 1 (trừ 20 của Binh) đã được ghi, bước 2 lỗi, 20 đơn vị tiền biến mất.

`IsolationDemo`:

```text
READ_UNCOMMITTED  while writer uncommitted=40.00   after writer committed=40.00
READ_COMMITTED    while writer uncommitted=100.00  after writer committed=40.00
REPEATABLE_READ   while writer uncommitted=100.00  after writer committed=100.00
```

`LostUpdateDemo`:

```text
read then write absolute value : final balance 80.00
atomic UPDATE balance = balance - ? : final balance 70.00
SELECT ... FOR UPDATE           : final balance 70.00, second writer waited >= 500 ms: true
optimistic version column       : first writer rows=1, second writer (stale version) rows=0
```

`DeadlockDemo`: một worker nhận `MySQLTransactionRollbackException SQLState=40001 errorCode=1213`, worker kia `committed` (worker nào thua thì không cố định). Hết thời gian chờ khóa: `SQLState=40001 errorCode=1205 after ~2000 ms`. Hàm thử lại: `attempts 1 and 2, final An=100.00 Binh=50.00`.

`BatchErrors`:

```text
default: BatchUpdateException counts [1, 1, -3, 1, 1], rows stored 4, cause Duplicate entry 'B' for key 'item.code'
rewriteBatchedStatements=true: BatchUpdateException counts [-3, -3, -3, -3, -3], rows stored 0, cause Duplicate entry 'B' for key 'item.code'
```

`BatchBenchmark` (2000 dòng, hai lần chạy trên máy tác giả):

```text
one statement per row, autocommit on       2000 rows in ~24000-26000 ms
one statement per row, one transaction     2000 rows in ~1300-1600 ms
addBatch/executeBatch, one transaction     2000 rows in ~1000-2100 ms
batch + rewriteBatchedStatements=true      2000 rows in ~200-250 ms
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 11, Failures: 0, Errors: 0`. Test tự khởi động MySQL riêng bằng Testcontainers nên cần Docker đang chạy.

## Dọn dẹp

```bash
docker compose down -v
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm | `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| Chuyển khoản vượt số dư vẫn thành công | MySQL cũ hơn 8.0.16 bỏ qua CHECK | Dùng image `mysql:8.4` của lab |
| `autocommit on` mất rất lâu | Mỗi dòng là một commit ghi đĩa | Bình thường, đó chính là điều bài muốn chứng minh; giảm `ROWS` nếu cần |
| `DeadlockDemo` không có nạn nhân | Hai luồng không giữ khóa đầu cùng lúc | Chạy lại; chốt `CountDownLatch` thường đảm bảo giao nhau |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker rồi chạy lại |
