# Bài 01 - Spring Data JPA và JpaRepository

Mã nguồn đi kèm bài học **Giới thiệu Spring Data JPA và JpaRepository** (chương 9: Spring Data JPA) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một `JpaRepository` chỉ là interface (đối tượng thật là proxy bọc `SimpleJpaRepository`), SQL sinh ra của `save` cho thực thể mới và thực thể tách rời (có `select` trước), cập nhật tự động trong giao dịch không cần `save`, chi phí SQL của `findById`, `existsById`, `count` và `getReferenceById`, truy vấn dẫn xuất `findByEmail` và `findByLevel`, `saveAll`, `deleteById` so với `deleteAllInBatch`, và cách Spring dịch lỗi trùng khóa thành `DataIntegrityViolationException`.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml`; quản lý Hibernate và Spring Data |
| MySQL | 8.4 (image `mysql:8.4`) | cổng 3307 của máy bạn |
| Docker | bất kỳ bản gần đây | MySQL cho demo và Testcontainers cho test |

## Cấu trúc

```text
01-jparepository/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml                       parent Boot 4.1.1, starter data-jpa, mysql-connector-j, test, testcontainers
  src/main/java/dev/canhdominich/boot/jpa/
    JpaApplication.java         @SpringBootApplication
    Level.java                  enum BASIC, ADVANCED
    Student.java                entity: id tự tăng, tên, email duy nhất, level
    StudentRepository.java      interface JpaRepository có hai truy vấn dẫn xuất
    SqlRecorder.java            StatementInspector ghi lại mọi câu SQL
    RepositoryDemo.java         chạy các thí nghiệm và in SQL
  src/test/java/dev/canhdominich/boot/jpa/
    MySqlLab.java, RepositoryBehaviourTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `RepositoryDemo` (đã bỏ log ở stderr):

```text
repository object: $Proxy (proxy), target SimpleJpaRepository
-- save
save(new Student) gives id true -> 1 statements
    insert into student (email,level,name) values (?,?,?)
save(detached student with an id) -> 2 statements
    select s1_0.id,s1_0.email,s1_0.level,s1_0.name from student s1_0 where s1_0.id=?
    update student set email=?,level=?,name=? where id=?
change inside one transaction, no save call -> 2 statements
    select s1_0.id,s1_0.email,s1_0.level,s1_0.name from student s1_0 where s1_0.id=?
    update student set email=?,level=?,name=? where id=?
-- reading
findById -> 1 statements
    select s1_0.id,s1_0.email,s1_0.level,s1_0.name from student s1_0 where s1_0.id=?
existsById -> 1 statements
    select count(*) from student s1_0 where s1_0.id=?
count -> 1 statements
    select count(*) from student s1_0
getReferenceById, id only used -> 0 statements
getReferenceById, then getName -> 1 statements
    select s1_0.id,s1_0.email,s1_0.level,s1_0.name from student s1_0 where s1_0.id=?
-- derived queries
findByEmail -> 1 statements
    select s1_0.id,s1_0.email,s1_0.level,s1_0.name from student s1_0 where s1_0.email=?
findByLevel -> 1 statements
    select s1_0.id,s1_0.email,s1_0.level,s1_0.name from student s1_0 where s1_0.level=?
-- saving and deleting several
saveAll of 3 -> 3 statements
    insert into student (email,level,name) values (?,?,?)
    insert into student (email,level,name) values (?,?,?)
    insert into student (email,level,name) values (?,?,?)
deleteById -> 2 statements
    select s1_0.id,s1_0.email,s1_0.level,s1_0.name from student s1_0 where s1_0.id=?
    delete from student where id=?
deleteAllInBatch -> 1 statements
    delete s1_0 from student s1_0
-- a duplicate email
exception chain: [DataIntegrityViolationException, ConstraintViolationException, SQLIntegrityConstraintViolationException]
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 6, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm | `docker compose down` ở lab trước, hoặc `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| `Communications link failure` | MySQL chưa sẵn sàng | Chờ `docker compose ps` báo `healthy` |
