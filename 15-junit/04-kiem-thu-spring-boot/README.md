# Bài 04 - Kiểm thử ứng dụng Spring Boot

Mã nguồn đi kèm bài học **Kiểm thử ứng dụng Spring Boot** (chương 15: JUnit) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một API sản phẩm nhỏ (controller, service, repository JPA) và năm kiểu test với phạm vi tăng dần: unit test không Spring với Mockito, `@WebMvcTest` với `MockMvc` và `@MockitoBean`, `@DataJpaTest` trên H2, `@SpringBootTest` với `MockMvc` và `@Transactional`, và `@DataJpaTest` trên MySQL thật bằng Testcontainers và `@ServiceConnection` (cho thấy MySQL so khớp tên không phân biệt hoa thường còn H2 thì có). Một bộ quan sát đo bộ nhớ đệm ngữ cảnh (hai lớp cùng cấu hình dùng chung một ngữ cảnh, thêm `@MockitoBean` tạo ngữ cảnh mới), số bean của từng loại lát cắt (226, 127, 81) và việc `@Transactional` ở lớp test hoàn tác yêu cầu `MockMvc` nhưng không hoàn tác yêu cầu qua máy chủ HTTP thật.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml` |
| Testcontainers | do Boot quản lý (2.x) | chạy MySQL cho `ProductMysqlTest` |
| Docker | bất kỳ bản gần đây | Docker Engine chạy được; `ProductMysqlTest` tự kéo và khởi động `mysql:8.4` bằng Testcontainers |

## Cấu trúc

```text
04-kiem-thu-spring-boot/
  pom.xml                       parent Boot 4.1.1, JPA, validation, H2, MySQL, các starter test, Testcontainers
  src/main/resources/application.properties
  src/main/java/dev/canhdominich/junit/catalog/
    CatalogApplication.java     @SpringBootApplication
    Product.java, ProductRepository.java        thực thể và repository JPA (findByName)
    ProductService.java, ProductNotFoundException.java
    ProductController.java, ProductRequest.java, ProductResponse.java   REST: GET, POST có kiểm tra hợp lệ
  src/test/java/dev/canhdominich/junit/catalog/
    ProductServiceTest.java         unit test với Mockito, không Spring
    ProductControllerWebTest.java   @WebMvcTest với MockMvc
    ProductRepositoryTest.java      @DataJpaTest trên H2
    ProductApiIntegrationTest.java  @SpringBootTest với MockMvc và @Transactional
    ProductMysqlTest.java           @DataJpaTest trên MySQL bằng Testcontainers
    Probes.java                     lớp thăm dò: ngữ cảnh, lát cắt, hoàn tác
    ProbeRunner.java                chạy một lớp bằng Launcher API
    SpringTestingMechanicsTest.java quan sát cơ chế và in báo cáo
```

## Chạy thí nghiệm

```bash
mvn -q test -Dtest=SpringTestingMechanicsTest
```

## Kết quả mong đợi

Báo cáo in ra từ `SpringTestingMechanicsTest`:

```text
-- the context cache
   two classes with the same configuration share one context: true
   adding a @MockitoBean makes another context: true
   the second class started much faster than the one with a new context: true
-- slices against the full application
   full application: 226 beans; has controller true, has repository true
   @WebMvcTest: 127 beans; has controller true, has repository false
   @DataJpaTest: 81 beans; has controller false, has repository true
-- @Transactional on the test class
   MockMvc: inside the test: 1
   MockMvc: in the next test: 0
   real HTTP server: HTTP status 201, inside the test: 1
   real HTTP server: in the next test: 1
```

Số bean (226, 127, 81) phụ thuộc phiên bản Spring Boot và các starter trong `pom.xml`; điều cố định là thứ tự (đầy đủ nhiều hơn `@WebMvcTest` nhiều hơn `@DataJpaTest`) và các dòng `true`, `0`, `1`. Chạy `mvn test` đầy đủ mất khoảng hai phút, phần lớn do khởi động MySQL trong Docker ở `ProductMysqlTest`.

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 17, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Surefire is going to kill self fork JVM` sau khi test chạy xong | Testcontainers dọn container chậm khi JVM thoát | Bỏ qua nếu cuối vẫn `BUILD SUCCESS`; thông báo này xuất hiện sau khi mọi test đã chạy |
| `ProductMysqlTest` lỗi kết nối Docker | Docker Engine chưa chạy hoặc không có quyền | Chạy Docker và thử `docker ps`; hoặc chạy các test khác bằng `-Dtest=` bỏ qua lớp này |
| Không thấy `@WebMvcTest` hay `@MockitoBean` trong IDE | Spring Boot 4 đổi tên gói của các annotation test | Dùng gói `org.springframework.boot.webmvc.test.autoconfigure` cho `@WebMvcTest` và `org.springframework.test.context.bean.override.mockito` cho `@MockitoBean` |
