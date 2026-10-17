# Bài 03 - Xây REST API với Spring Boot

Mã nguồn đi kèm bài học **Xây REST API với Spring Boot** (chương 8: Spring Boot) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một API tài nguyên khóa học (tạo, đọc, sửa, xóa, phân trang) trên Spring Boot, với `@Valid`, mã trạng thái đúng (201 kèm `Location`, 204, 404, 405, 406, 415), lỗi thống nhất dạng `ProblemDetail` (`application/problem+json`) kể cả lỗi kiểm tra dữ liệu, JSON sai cú pháp và tham số phân trang sai, và hành vi mặc định của Jackson với trường JSON lạ.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
03-rest-api/
  pom.xml
  src/main/java/dev/canhdominich/boot/rest/
    RestApplication.java        @SpringBootApplication
    Course.java                 record tài nguyên (id, tiêu đề, giá)
    CourseRequest.java          record dữ liệu vào, có ràng buộc Bean Validation
    PageResponse.java           record một trang kết quả
    CourseNotFoundException.java ngoại lệ nghiệp vụ
    CourseStore.java            kho dữ liệu trong bộ nhớ
    CourseController.java       các endpoint /api/courses
    ApiExceptionHandler.java    @RestControllerAdvice trả ProblemDetail
    RestDemo.java               khởi động ứng dụng, gửi yêu cầu HTTP và in kết quả
  src/test/java/dev/canhdominich/boot/rest/
    RestBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `RestDemo` (đã bỏ log ở stderr; header `Allow` được sắp xếp để kết quả ổn định):

```text
POST /api/courses {"title":"Spring Boot","price":100} -> 201 application/json Location: /api/courses/1 | {"id":1,"title":"Spring Boot","price":100}
POST /api/courses {"title":"","price":-1} -> 400 application/problem+json | {"detail":"Request validation failed","status":400,"title":"Bad Request","errors":{"price":"must be greater than or equal to 0","title":"must not be blank"}}
POST /api/courses {"title":"Extra field ignored","price":5,"unknown":true} -> 201 application/json Location: /api/courses/2 | {"id":2,"title":"Extra field ignored","price":5}
POST /api/courses {"title": -> 400 application/problem+json | {"detail":"Failed to read request","status":400,"title":"Bad Request"}
POST /api/courses Spring Boot -> 415 application/problem+json | {"detail":"Content-Type 'text/plain;charset=UTF-8' is not supported.","status":415,"title":"Unsupported Media Type"}
GET /api/courses/1 -> 200 application/json | {"id":1,"title":"Spring Boot","price":100}
GET /api/courses/99 -> 404 application/problem+json | {"detail":"Course 99 does not exist","status":404,"title":"Not Found"}
GET /api/courses?page=1&size=5 -> 200 application/json | {"items":[{"id":6,"title":"Course 7","price":7},{"id":7,"title":"Course 8","price":8},{"id":8,"title":"Course 9","price":9},{"id":9,"title":"Course 10","price":10},{"id":10,"title":"Course 11","price":11}],"page":1,"size":5,"total":11}
GET /api/courses?size=0 -> 400 application/problem+json | {"detail":"Validation failure","status":400,"title":"Bad Request"}
GET /api/courses?size=abc -> 400 application/problem+json | {"detail":"Failed to convert 'size' with value: 'abc'","status":400,"title":"Bad Request"}
PUT /api/courses/1 {"title":"Spring Boot 4","price":150} -> 200 application/json | {"id":1,"title":"Spring Boot 4","price":150}
PUT /api/courses/99 {"title":"x","price":1} -> 404 application/problem+json | {"detail":"Course 99 does not exist","status":404,"title":"Not Found"}
DELETE /api/courses/1 -> 204
DELETE /api/courses/1 -> 404 application/problem+json | {"detail":"Course 1 does not exist","status":404,"title":"Not Found"}
PATCH /api/courses/2 {} -> 405 application/problem+json Allow: DELETE, GET, PUT | {"detail":"Method 'PATCH' is not supported.","status":405,"title":"Method Not Allowed"}
GET /api/courses/2 -> 406 application/problem+json | {"detail":"Acceptable representations: [application/json, application/*+json].","status":406,"title":"Not Acceptable"}
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 5, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Số `total` khác bài | Bộ đếm id và kho dữ liệu dùng chung giữa các lần chạy trong cùng một tiến trình | Mỗi lần `mvn compile exec:exec` là một tiến trình mới; chạy đúng lệnh trong README |
| `Port ... already in use` khi tự chạy `RestApplication` | Cổng 8080 đang bị chiếm | `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
