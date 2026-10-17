# Bài 03 - Giao dịch, quan hệ và N+1 với Spring Data JPA

Mã nguồn đi kèm bài học **Giao dịch, quan hệ và N+1 với Spring Data JPA** (chương 9: Spring Data JPA) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: `@Transactional` rollback với ngoại lệ không kiểm tra nhưng commit với ngoại lệ kiểm tra (và `rollbackFor`), `REQUIRED` so với `REQUIRES_NEW` khi người gọi thất bại, cái bẫy gọi phương thức `@Transactional` qua `this` (proxy bị bỏ qua), `readOnly = true` bỏ qua cập nhật, `UnexpectedRollbackException` khi người gọi nuốt lỗi của giao dịch tham gia, vấn đề N+1 qua repository với hai cách sửa (`@EntityGraph`, `join fetch`), và `spring.jpa.open-in-view` quyết định việc nạp lười ngoài giao dịch trong một yêu cầu web.

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
03-giao-dich-quan-he-n-mot/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/java/dev/canhdominich/boot/tx/
    TxApplication.java          @SpringBootApplication
    Account.java, AccountRepository.java      tài khoản có số dư
    AuditEntry.java, AuditRepository.java     bản ghi kiểm toán
    AuditService.java           ghi kiểm toán tham gia hoặc REQUIRES_NEW
    TransferService.java        các phương thức @Transactional khác nhau để thử
    Course.java, Lesson.java    quan hệ một-nhiều
    CourseRepository.java       findAll, @EntityGraph, join fetch
    CourseService.java          đếm bài học theo bốn cách
    LessonController.java       endpoint web đọc quan hệ lười không có giao dịch
    SqlRecorder.java            StatementInspector ghi lại SQL
    TxDemo.java                 chạy mọi thí nghiệm
  src/test/java/dev/canhdominich/boot/tx/
    MySqlLab.java, TxBehaviourTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `TxDemo` (đã bỏ log ở stderr):

```text
-- which exceptions roll a transaction back
unchecked exception after the debit: IllegalStateException, balance after = 100
checked exception after the debit: IOException, balance after = 70
checked exception with rollbackFor = Exception.class: IOException, balance after = 100
-- propagation
audit joins the transaction, caller fails: balance 100, audit rows 0
audit in REQUIRES_NEW, caller fails: balance 100, audit rows 1
-- the proxy and self-invocation
REQUIRES_NEW method called with this.: transaction active = false
REQUIRES_NEW method called through another bean: transaction active = true
-- readOnly
change inside readOnly = true: balance 100, update statements 0
-- a failure inside a joined transaction, swallowed by the caller
outer commit -> UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
-- relations and N+1 through repositories
findAll then lessons inside a transaction -> 30 lessons, 11 statements
@EntityGraph(attributePaths = "lessons") -> 30 lessons, 1 statements
join fetch in @Query -> 30 lessons, 1 statements
lazy collection with no transaction and no web request -> LazyInitializationException
-- open-in-view
web request, no transaction, open-in-view default: 200 lessons 30
web request, no transaction, spring.jpa.open-in-view=false: 500
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
| Dòng cuối trả 200 thay vì 500 | Thuộc tính `spring.jpa.open-in-view=false` chưa được đặt cho ứng dụng thứ hai | Chạy đúng `TxDemo`; xem phần cuối của `run` |
