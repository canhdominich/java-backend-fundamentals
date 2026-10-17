# Bài 05 - Criteria, bộ nhớ đệm và kế thừa

Mã nguồn đi kèm bài học **Criteria, Caching và kế thừa** (chương 5: Hibernate) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: truy vấn động bằng Criteria API (chỉ thêm điều kiện khi người dùng nhập, đếm tổng cho phân trang, giá trị nhập đi qua tham số, lỗi chính tả tên thuộc tính), bộ nhớ đệm cấp một và cấp hai với Ehcache qua JCache (cả cache truy vấn, và dữ liệu cũ khi chương trình khác sửa dòng), và ba chiến lược kế thừa `SINGLE_TABLE`, `JOINED`, `TABLE_PER_CLASS` với bảng và SQL mà mỗi chiến lược sinh ra.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| MySQL | 8.4 (image `mysql:8.4`) |  |
| Hibernate ORM | 7.4.13.Final (kèm `hibernate-jcache`) | khai báo trong `pom.xml` |
| Ehcache | 3.12.0, classifier `jakarta` | nhà cung cấp cache qua JCache |
| MySQL Connector/J | 26.7.0 | khai báo trong `pom.xml` |
| Docker | bất kỳ bản gần đây | MySQL cho demo và Testcontainers cho test |

## Cấu trúc

```text
05-criteria-cache-ke-thua/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/java/dev/canhdominich/hibernate/advanced/
    DbConfig.java, SqlRecorder.java, UnitOfWork.java   như bài trước
    Course.java                entity khóa học, có @Cacheable và @Cache
    CourseSearch.java          record chứa các bộ lọc tùy chọn
    CourseQueries.java         truy vấn Criteria động và truy vấn đếm
    Factories.java             dựng SessionFactory, có bản bật cache cấp hai
    SampleData.java            30 khóa học mẫu
    CriteriaDemo.java          thí nghiệm Criteria
    CacheDemo.java             thí nghiệm bộ nhớ đệm
    InheritanceDemo.java       thí nghiệm ba chiến lược kế thừa
    single/ joined/ tpc/       mỗi thư mục: Payment, CardPayment, TransferPayment (tpc còn IdentityBase, IdentityChild)
  src/test/java/dev/canhdominich/hibernate/advanced/
    MySqlLab.java, AdvancedBehaviourTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.hibernate.advanced.CriteriaDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.advanced.CacheDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.advanced.InheritanceDemo
```

Hibernate ghi log khởi động ra stderr, và lần đầu có vài cảnh báo `Table ... doesn't exist` do `create-drop` xóa bảng chưa có: vô hại. Dưới đây chỉ chép các dòng kết quả của chương trình.

## Kết quả mong đợi

`CriteriaDemo`:

```text
no filter: 5 courses on the first page
SQL of the criteria query
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 order by c1_0.id limit ?,?
level only: 5 courses on the first page
SQL of the criteria query
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.level=? order by c1_0.id limit ?,?
title, level and price range: 3 courses on the first page
SQL of the criteria query
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.title like ? escape '' and c1_0.level=? and c1_0.price_vnd>=? and c1_0.price_vnd<=? order by c1_0.id limit ?,?
total for the same filter: 10
SQL of the count query
  select count(c1_0.id) from course c1_0 where c1_0.level=?
title filter "' or 1=1 --": 0 courses
SQL (the value travels as a parameter)
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.title like ? escape '' order by c1_0.id limit ?,?
misspelled attribute: PathElementException: Could not resolve attribute 'titel' of 'dev.canhdominich.hibernate.advanced.Course'
```

`CacheDemo`:

```text
find twice in one Session -> 1 statements
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.id=?
L2 hits=0 misses=0 puts=0
find in two Sessions, use_second_level_cache=false -> 2 statements
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.id=?
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.id=?
L2 hits=1 misses=1 puts=1
find in two Sessions, property not set, jcache and ehcache on the classpath -> 1 statements
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.id=?
L2 hits=1 misses=1 puts=1
find in two Sessions, everything configured -> 1 statements
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.id=?
query cache hits=1 misses=1 puts=1
cacheable query run in two Sessions -> 1 statements
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.level=?
price read after a change made through Hibernate: 777000
read after the change -> 0 statements
  (no SQL)
price read after another program wrote 1 to the row: 500000
read after the outside change -> 0 statements
  (no SQL)
price read after evictEntityData: 1
read after eviction -> 1 statements
  select c1_0.id,c1_0.level,c1_0.price_vnd,c1_0.title from course c1_0 where c1_0.id=?
```

`InheritanceDemo`:

```text
== SINGLE_TABLE ==
  table payment: amount_vnd not-null, bank_reference null, card_last4 null, id not-null, kind not-null,
  inserts
  insert into payment (amount_vnd,card_last4,kind) values (?,?,'CARD')
  insert into payment (amount_vnd,bank_reference,kind) values (?,?,'TRANSFER')
  from Payment: 2 rows
  SQL of the polymorphic query
  select p1_0.id,p1_0.kind,p1_0.amount_vnd,p1_0.card_last4,p1_0.bank_reference from payment p1_0
  SQL of the query on one subclass
  select cp1_0.id,cp1_0.amount_vnd,cp1_0.card_last4 from payment cp1_0 where cp1_0.kind='CARD'


== JOINED ==
  table payment: amount_vnd not-null, id not-null,
  table card_payment: card_last4 null, id not-null,
  table transfer_payment: bank_reference null, id not-null,
  inserts
  insert into payment (amount_vnd) values (?)
  insert into card_payment (card_last4,id) values (?,?)
  insert into payment (amount_vnd) values (?)
  insert into transfer_payment (bank_reference,id) values (?,?)
  from Payment: 2 rows
  SQL of the polymorphic query
  select p1_0.id,case when p1_1.id is not null then 1 when p1_2.id is not null then 2 end,p1_0.amount_vnd,p1_1.card_last4,p1_2.bank_reference from payment p1_0 left join card_payment p1_1 on p1_0.id=p1_1.id left join transfer_payment p1_2 on p1_0.id=p1_2.id
  SQL of the query on one subclass
  select cp1_0.id,cp1_1.amount_vnd,cp1_0.card_last4 from card_payment cp1_0 join payment cp1_1 on cp1_0.id=cp1_1.id
== TABLE_PER_CLASS ==
  table card_payment: amount_vnd not-null, card_last4 null, id not-null,
  table transfer_payment: amount_vnd not-null, bank_reference null, id not-null,
  inserts
  insert into card_payment (amount_vnd,card_last4,id) values (?,?,?)
  insert into transfer_payment (amount_vnd,bank_reference,id) values (?,?,?)
  from Payment: 2 rows
  SQL of the polymorphic query
  select p1_0.id,p1_0.clazz_,p1_0.amount_vnd,p1_0.card_last4,p1_0.bank_reference from (select amount_vnd, id, card_last4, null as bank_reference, 1 as clazz_ from card_payment union all select amount_vnd, id, null as card_last4, bank_reference, 2 as clazz_ from transfer_payment) p1_0
  SQL of the query on one subclass
  select cp1_0.id,cp1_0.amount_vnd,cp1_0.card_last4 from card_payment cp1_0
== TABLE_PER_CLASS with IDENTITY ==
  MappingException: Cannot use identity column key generation with <union-subclass> mapping for: dev.canhdominich.hibernate.advanced.tpc.IdentityChild
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 7, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
docker compose down
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm (ví dụ bởi lab trước chưa tắt) | `docker compose down` ở lab trước, hoặc `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| Maven báo không tải được `jaxb-api` hoặc `istack-commons-runtime` | Ehcache kéo thư viện JAXB cũ từ kho bị chặn | Giữ khối `exclusions` cho `jaxb-runtime` và dòng khai báo `org.glassfish.jaxb:jaxb-runtime:4.0.9` trong `pom.xml` |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker rồi chạy lại |
