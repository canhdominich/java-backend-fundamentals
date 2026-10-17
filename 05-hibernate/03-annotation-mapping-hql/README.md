# Bài 03 - Annotations, Mapping và HQL

Mã nguồn đi kèm bài học **Annotations, Mapping và HQL** (chương 5: Hibernate) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: ánh xạ một mô hình lớp học (lớp, sinh viên, khóa học, đăng ký học) bằng annotation và xem bảng cùng khóa ngoại Hibernate tạo ra, tên bảng mặc định so với `CamelCaseToUnderscoresNamingStrategy`, enum lưu dạng `ORDINAL` và `STRING`, phía sở hữu của quan hệ hai chiều, `cascade` và `orphanRemoval`, kiểu nạp mặc định của `@ManyToOne`, HQL (tham số, chiếu, DTO bằng `record`, tổng hợp, phân trang, join), `LazyInitializationException` và vấn đề N+1 cùng cách sửa bằng `join fetch`.

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
03-annotation-mapping-hql/
  docker-compose.yml           MySQL 8.4, database shop, user app (cổng 3307)
  pom.xml
  src/main/java/dev/canhdominich/hibernate/mapping/
    DbConfig.java, SqlRecorder.java, UnitOfWork.java   như bài trước
    Student.java               entity chính: cột, enum, Embedded, ManyToOne, OneToMany, ManyToMany
    Address.java               lớp @Embeddable (đường, thành phố)
    Classroom.java             phía nghịch (mappedBy) của quan hệ với Student
    Course.java                khóa học
    Enrollment.java            đăng ký học: ManyToOne tới Student (mặc định EAGER) và Course (LAZY)
    Grade.java                 enum lưu dạng ORDINAL và STRING
    Factories.java             dựng SessionFactory, tùy chọn quy tắc đặt tên snake_case
    SampleData.java            nạp dữ liệu mẫu cho các thí nghiệm HQL và nạp lười
    MappingDemo.java           in cấu trúc bảng, khóa ngoại, kiểu enum
    RelationshipsDemo.java     phía sở hữu, cascade, orphanRemoval, EAGER và LAZY
    HqlDemo.java               truy vấn HQL và SQL được sinh ra
    LazyLoadingDemo.java       LazyInitializationException, N+1, join fetch
  src/test/java/dev/canhdominich/hibernate/mapping/
    MySqlLab.java, MappingBehaviourTest.java
```

## Chạy thí nghiệm

```bash
docker compose up -d
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.hibernate.mapping.MappingDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.mapping.RelationshipsDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.mapping.HqlDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.hibernate.mapping.LazyLoadingDemo
```

Hibernate ghi log khởi động ra stderr; các dòng bắt đầu bằng `--` hoặc hai dấu cách là kết quả của chương trình.

## Kết quả mong đợi

`MappingDemo`:

```text
-- table names with Hibernate's default naming: [Classroom, Course, Enrollment, Grade, student, student_course]
-- table names with CamelCaseToUnderscoresNamingStrategy: [classroom, course, enrollment, grade, student, student_course]
-- columns and constraints of student
  birth_date date
  classroom_id bigint
  id bigint NOT NULL
  city varchar(60)
  email varchar(100) NOT NULL
  full_name varchar(100) NOT NULL
  street varchar(100)
  level enum('ADVANCED','BASIC') NOT NULL
  foreign key classroom_id -> classroom
-- columns and constraints of enrollment
  enrolled_on date NOT NULL
  course_id bigint NOT NULL
  id bigint NOT NULL
  student_id bigint NOT NULL
  foreign key course_id -> course
  foreign key student_id -> student
-- columns and constraints of student_course
  course_id bigint NOT NULL
  student_id bigint NOT NULL
  foreign key course_id -> course
  foreign key student_id -> student
-- columns and constraints of classroom
  id bigint NOT NULL
  name varchar(50) NOT NULL
-- enum stored as ORDINAL: 2, as STRING: MERIT
```

`RelationshipsDemo`:

```text
-- classroom.getStudents().add(student) only: student.classroom in the database = null
-- classroom.addStudent(student): student.classroom name = Both sides set by addStudent
-- persist(student) with two new enrollments: CascadeType.ALL persists the children
  insert into course (title) values (?)
  insert into student (city,street,birth_date,classroom_id,email,full_name,level) values (?,?,?,?,?,?,?)
  insert into enrollment (course_id,enrolled_on,student_id) values (?,?,?)
  insert into enrollment (course_id,enrolled_on,student_id) values (?,?,?)
-- remove(student): the children go first
  select s1_0.id,s1_0.city,s1_0.street,s1_0.birth_date,s1_0.classroom_id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
  select e1_0.student_id,e1_0.id,e1_0.course_id,e1_0.enrolled_on from enrollment e1_0 where e1_0.student_id=?
  delete from student_course where student_id=?
  delete from enrollment where id=?
  delete from enrollment where id=?
  delete from student where id=?
-- removing one enrollment from the list (orphanRemoval = true)
  select s1_0.id,s1_0.city,s1_0.street,s1_0.birth_date,s1_0.classroom_id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.id=?
  select e1_0.student_id,e1_0.id,e1_0.course_id,e1_0.enrolled_on from enrollment e1_0 where e1_0.student_id=?
  delete from enrollment where id=?
-- find(Enrollment): student is ManyToOne (EAGER by default), course is declared LAZY
  select e1_0.id,e1_0.course_id,e1_0.enrolled_on,e1_0.student_id,s1_0.id,s1_0.city,s1_0.street,s1_0.birth_date,s1_0.classroom_id,s1_0.email,s1_0.full_name,s1_0.level from enrollment e1_0 join student s1_0 on s1_0.id=e1_0.student_id where e1_0.id=?
-- first access to the lazy course
  select c1_0.id,c1_0.title from course c1_0 where c1_0.id=?
```

`HqlDemo`:

```text
-- parameter binding: 6 advanced students
-- SQL of the parameterised query
  select s1_0.id,s1_0.city,s1_0.street,s1_0.birth_date,s1_0.classroom_id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 where s1_0.level=? order by s1_0.full_name
-- projection of two fields returns 2 Object[] rows, first = Student 1 / BASIC
-- SQL of the projection (no entity is created)
  select s1_0.full_name,s1_0.level from student s1_0 where s1_0.id<=2 order by s1_0.id
-- DTO CityCount[city=Da Nang, students=10]
-- DTO CityCount[city=Ha Noi, students=10]
-- SQL of the aggregate into a record
  select s1_0.city,count(s1_0.id) from student s1_0 group by s1_0.city order by s1_0.city
-- page of 5 starting at Student[6, Student 6]
-- SQL of the paginated query
  select s1_0.id,s1_0.city,s1_0.street,s1_0.birth_date,s1_0.classroom_id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 order by s1_0.id limit ?,?
-- join through the collection: 3 rows
-- SQL of the join (HQL names classes and fields, SQL names tables and columns)
  select s1_0.id,s1_0.city,s1_0.street,s1_0.birth_date,s1_0.classroom_id,s1_0.email,s1_0.full_name,s1_0.level from student s1_0 join enrollment e1_0 on s1_0.id=e1_0.student_id join course c2_0 on c2_0.id=e1_0.course_id where c2_0.title=? and s1_0.id<=3
-- count = 20
```

`LazyLoadingDemo`:

```text
-- collection touched after the Session closed: LazyInitializationException
-- 20 students, 40 enrollments counted by touching each collection
-- statements sent: 21
-- 20 students, 40 enrollments with join fetch
-- statements sent: 1
-- SQL
  select distinct s1_0.id,s1_0.city,s1_0.street,s1_0.birth_date,s1_0.classroom_id,s1_0.email,e1_0.student_id,e1_0.id,e1_0.course_id,e1_0.enrolled_on,s1_0.full_name,s1_0.level from student s1_0 join enrollment e1_0 on s1_0.id=e1_0.student_id
```

Cột `level` được Hibernate 7 ánh xạ thành kiểu `ENUM` của MySQL (`enum('ADVANCED','BASIC')`), không phải `VARCHAR`. Tên bảng mặc định giữ nguyên chữ hoa của tên lớp (`Classroom`), và trên MySQL chạy ở Linux tên bảng phân biệt chữ hoa chữ thường.

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 4, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Bind for 0.0.0.0:3307 failed: port is already allocated` | Cổng 3307 đã bị chiếm (ví dụ bởi lab trước chưa tắt) | `docker compose down` ở lab trước, hoặc `MYSQL_PORT=3308 docker compose up -d` rồi `export JDBC_URL=jdbc:mysql://localhost:3308/shop` |
| Danh sách bảng có thêm bảng của lab khác | Cùng database `shop` được dùng chung | Bỏ qua: chương trình chỉ in các bảng của bài này |
| `Could not find a valid Docker environment` khi `mvn test` | Docker chưa chạy | Bật Docker rồi chạy lại |
