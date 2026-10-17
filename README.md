# Java Backend Fundamentals

Mã nguồn đi kèm khóa học **Java Backend: JDBC, Servlet, Hibernate và Spring**. Mỗi bài học có thực hành code đều có một thư mục riêng ở đây, chạy được độc lập, kèm README ghi môi trường, cách chạy và kết quả mong đợi. Số liệu và thông báo lỗi trong bài học được đo từ chính các thư mục này.

## Cách dùng

1. Đọc bài học trên nền tảng; bài có liên kết thẳng tới thư mục mã nguồn tương ứng.
2. Clone repo một lần:

   ```bash
   git clone https://github.com/canhdominich/java-backend-fundamentals.git
   ```

3. Vào thư mục của bài, đọc `README.md` trong đó rồi chạy theo hướng dẫn.

## Quy ước thư mục

```text
<số chương>-<chủ đề>/<số bài>-<tên bài>/
```

Số chương và số bài khớp thứ tự trong khóa học. Mỗi thư mục bài là một dự án Maven độc lập có `README.md`, `pom.xml` và, khi cần dịch vụ bên ngoài, `docker-compose.yml`.

## Yêu cầu chung

| Thành phần | Phiên bản |
|---|---|
| JDK | 17 trở lên |
| Maven | 3.9 trở lên |
| Docker và Docker Compose | Cần cho các bài dùng cơ sở dữ liệu và cho bộ test dùng Testcontainers |

## Các bài đã có

| Chương | Bài | Thư mục |
|---|---|---|
| 1. Giới thiệu | Kiểm tra môi trường học | [01-nhap-mon/01-kiem-tra-moi-truong](01-nhap-mon/01-kiem-tra-moi-truong) |
| 1. Giới thiệu | Từ socket tới servlet: Core Java cho bạn gì, Advanced Java thêm gì | [01-nhap-mon/02-tu-socket-den-servlet](01-nhap-mon/02-tu-socket-den-servlet) |
| 2. JDBC | Giới thiệu JDBC và JDBC Driver | [02-jdbc/01-gioi-thieu-jdbc](02-jdbc/01-gioi-thieu-jdbc) |
| 2. JDBC | Kết nối MySQL bằng JDBC | [02-jdbc/02-ket-noi-mysql](02-jdbc/02-ket-noi-mysql) |
| 2. JDBC | CRUD và các loại Statement | [02-jdbc/03-crud-va-statement](02-jdbc/03-crud-va-statement) |
| 2. JDBC | Transaction và Batch | [02-jdbc/04-transaction-va-batch](02-jdbc/04-transaction-va-batch) |
| 2. JDBC | DAO và Repository thuần JDBC | [02-jdbc/05-dao-va-repository](02-jdbc/05-dao-va-repository) |
| 3. Servlet | Giới thiệu Servlet và cách hoạt động | [03-servlet/01-gioi-thieu-servlet](03-servlet/01-gioi-thieu-servlet) |
| 3. Servlet | Vòng đời của Servlet | [03-servlet/02-vong-doi-servlet](03-servlet/02-vong-doi-servlet) |
| 3. Servlet | Request, Response và Session | [03-servlet/03-request-response-session](03-servlet/03-request-response-session) |
| 3. Servlet | Config, Dispatcher và Filters | [03-servlet/04-config-dispatcher-filters](03-servlet/04-config-dispatcher-filters) |
| 4. JSP | Giới thiệu và kiến trúc JSP | [04-jsp/01-gioi-thieu-jsp](04-jsp/01-gioi-thieu-jsp) |
| 4. JSP | Vòng đời và thành phần JSP | [04-jsp/02-vong-doi-thanh-phan-jsp](04-jsp/02-vong-doi-thanh-phan-jsp) |
| 4. JSP | So sánh Servlet và JSP, ứng dụng MVC đăng nhập | [04-jsp/03-servlet-va-jsp-mvc](04-jsp/03-servlet-va-jsp-mvc) |
| 5. Hibernate | Giới thiệu và kiến trúc Hibernate | [05-hibernate/01-gioi-thieu-kien-truc](05-hibernate/01-gioi-thieu-kien-truc) |
| 5. Hibernate | Cấu hình và CRUD với Hibernate | [05-hibernate/02-cau-hinh-va-crud](05-hibernate/02-cau-hinh-va-crud) |
| 5. Hibernate | Annotations, Mapping và HQL | [05-hibernate/03-annotation-mapping-hql](05-hibernate/03-annotation-mapping-hql) |
| 5. Hibernate | Hiệu năng Hibernate và khóa dữ liệu | [05-hibernate/04-hieu-nang-va-lock](05-hibernate/04-hieu-nang-va-lock) |
| 5. Hibernate | Criteria, bộ nhớ đệm và kế thừa | [05-hibernate/05-criteria-cache-ke-thua](05-hibernate/05-criteria-cache-ke-thua) |
| 6. Spring Core | IoC và Dependency Injection | [06-spring-core/01-ioc-va-di](06-spring-core/01-ioc-va-di) |
| 6. Spring Core | Container và cách cấu hình bean | [06-spring-core/02-container-va-cau-hinh](06-spring-core/02-container-va-cau-hinh) |
| 6. Spring Core | Dependency Injection và Autowiring | [06-spring-core/03-dependency-injection](06-spring-core/03-dependency-injection) |
| 6. Spring Core | Vòng đời, scope và SpEL | [06-spring-core/04-vong-doi-scope-spel](06-spring-core/04-vong-doi-scope-spel) |
| 7. Spring MVC | DispatcherServlet và vòng đời một yêu cầu | [07-spring-mvc/01-dispatcherservlet](07-spring-mvc/01-dispatcherservlet) |
| 7. Spring MVC | Controller, mapping và tham số yêu cầu | [07-spring-mvc/02-controller-va-tham-so](07-spring-mvc/02-controller-va-tham-so) |
| 7. Spring MVC | View, validation và xử lý lỗi | [07-spring-mvc/03-view-validation-loi](07-spring-mvc/03-view-validation-loi) |
| 8. Spring Boot | Giới thiệu và kiến trúc Spring Boot | [08-spring-boot/01-gioi-thieu-kien-truc](08-spring-boot/01-gioi-thieu-kien-truc) |
| 8. Spring Boot | Cấu hình, starter và annotation | [08-spring-boot/02-cau-hinh-starters-annotations](08-spring-boot/02-cau-hinh-starters-annotations) |
| 8. Spring Boot | Xây REST API với Spring Boot | [08-spring-boot/03-rest-api](08-spring-boot/03-rest-api) |
| 8. Spring Boot | Actuator, DevTools và logging | [08-spring-boot/04-actuator-devtools-logging](08-spring-boot/04-actuator-devtools-logging) |
| 9. Spring Data JPA | Spring Data JPA và JpaRepository | [09-spring-data-jpa/01-jparepository](09-spring-data-jpa/01-jparepository) |
| 9. Spring Data JPA | Truy vấn, phân trang và chiếu dữ liệu | [09-spring-data-jpa/02-truy-van-phan-trang](09-spring-data-jpa/02-truy-van-phan-trang) |
| 9. Spring Data JPA | Giao dịch, quan hệ và N+1 với Spring Data JPA | [09-spring-data-jpa/03-giao-dich-quan-he-n-mot](09-spring-data-jpa/03-giao-dich-quan-he-n-mot) |
| 10. Spring JDBC | Spring JDBC và JdbcTemplate | [10-spring-jdbc/01-jdbctemplate](10-spring-jdbc/01-jdbctemplate) |
| 10. Spring JDBC | RowMapper, tham số có tên và batch | [10-spring-jdbc/02-rowmapper-batch](10-spring-jdbc/02-rowmapper-batch) |
| 11. Spring Security | Spring Security: kiến trúc và cấu hình mặc định | [11-spring-security/01-kien-truc-mac-dinh](11-spring-security/01-kien-truc-mac-dinh) |
| 11. Spring Security | Cấu hình bảo mật, mã hóa mật khẩu và vai trò | [11-spring-security/02-cau-hinh-ma-hoa-vai-tro](11-spring-security/02-cau-hinh-ma-hoa-vai-tro) |
| 11. Spring Security | JWT: xác thực không trạng thái cho API | [11-spring-security/03-jwt](11-spring-security/03-jwt) |
| 12. AOP | AOP trong Spring: aspect và proxy | [12-aop/01-aop-proxy](12-aop/01-aop-proxy) |
| 12. AOP | Các loại advice và quản lý giao dịch | [12-aop/02-advice-va-giao-dich](12-aop/02-advice-va-giao-dich) |
| 13. Spring Cloud | Service discovery với Eureka và chia tải phía client | [13-spring-cloud/01-service-discovery](13-spring-cloud/01-service-discovery) |
| 13. Spring Cloud | OpenFeign và circuit breaker | [13-spring-cloud/02-openfeign-va-circuit-breaker](13-spring-cloud/02-openfeign-va-circuit-breaker) |
| 13. Spring Cloud | API Gateway với Spring Cloud Gateway | [13-spring-cloud/03-api-gateway](13-spring-cloud/03-api-gateway) |
| 13. Spring Cloud | Config Server và cấu hình tập trung | [13-spring-cloud/04-config-server](13-spring-cloud/04-config-server) |
| 13. Spring Cloud | Distributed tracing với Micrometer Tracing | [13-spring-cloud/05-distributed-tracing](13-spring-cloud/05-distributed-tracing) |
| 14. Microservices | Monolith và microservices: đo cái giá của việc tách | [14-microservices/01-monolith-va-microservices](14-microservices/01-monolith-va-microservices) |
| 14. Microservices | Nhất quán giữa các dịch vụ: bù trừ và idempotency | [14-microservices/02-saga-va-idempotency](14-microservices/02-saga-va-idempotency) |
| 14. Microservices | Đóng gói và chạy microservices bằng Docker Compose | [14-microservices/03-docker-compose](14-microservices/03-docker-compose) |
| 15. JUnit và kiểm thử | JUnit cơ bản: vòng đời, kết quả và thứ tự | [15-junit/01-junit-co-ban](15-junit/01-junit-co-ban) |
| 15. JUnit và kiểm thử | Assertions, kiểm thử tham số hóa và TDD | [15-junit/02-assertions-parameterized-tdd](15-junit/02-assertions-parameterized-tdd) |
| 15. JUnit và kiểm thử | Mockito: mock, stub, verify và những chỗ mock đánh lừa bạn | [15-junit/03-mockito](15-junit/03-mockito) |
| 15. JUnit và kiểm thử | Kiểm thử ứng dụng Spring Boot | [15-junit/04-kiem-thu-spring-boot](15-junit/04-kiem-thu-spring-boot) |
