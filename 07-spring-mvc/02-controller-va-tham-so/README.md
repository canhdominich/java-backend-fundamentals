# Bài 02 - Controller, mapping và tham số yêu cầu

Mã nguồn đi kèm bài học **Controller, Mapping và tham số request** (chương 7: Spring MVC) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: mười chín yêu cầu HTTP thật tới các controller để xem `@PathVariable`, `@RequestParam` (kể cả giá trị mặc định, thiếu giá trị và sai kiểu), `@RequestHeader`, `@ModelAttribute` cho form, đường dẫn cố định thắng mẫu, `@Controller` trả view so với `@RestController` trả JSON (Jackson 3), `ResponseEntity` với 201 và 404, và các lỗi 415, 406 từ việc thương lượng kiểu nội dung; kèm ngoại lệ mà Spring ném ra ở mỗi lỗi.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Framework | 7.0.9 | khai báo trong `pom.xml` |
| Tomcat nhúng | 11.0.27 | khai báo trong `pom.xml` |
| Jackson | 3.2.3 (`tools.jackson`) | chuyển đối tượng thành JSON |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-controller-va-tham-so/
  pom.xml
  src/main/java/dev/canhdominich/spring/mvc/
    Trace.java, EmbeddedServer.java, ServerMain.java   như bài trước (không còn filter)
    Registration.java           đối tượng nhận dữ liệu form
    StudentDto.java             record dùng cho JSON
    PageController.java         @Controller: đường dẫn, tham số, form, header, view
    ApiController.java          @RestController: JSON, ResponseEntity, consumes và produces
    ExceptionSpy.java           ghi tên ngoại lệ mà Spring ném ra, không xử lý nó
    WebConfig.java              @EnableWebMvc, view resolver, gắn ExceptionSpy
    ParamsDemo.java             gửi mười chín yêu cầu và in kết quả
  src/main/webapp/WEB-INF/views/
    students.jsp                view của /pages/view
  src/test/java/dev/canhdominich/spring/mvc/
    ParamsBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.spring.mvc.ParamsDemo
```

## Kết quả mong đợi

Kết quả của `ParamsDemo` (đã bỏ log ở stderr):

```text
GET /pages/students/5 -> 200 text/plain | student 5
GET /pages/students/new -> 200 text/plain | form for a new student
GET /pages/products/J-77/reviews/3 -> 200 text/plain | review 3 of J-77
GET /pages/search?q=java&page=2 -> 200 text/plain | q=java page=2
GET /pages/search?q=java -> 200 text/plain | q=java page=1
GET /pages/search -> 400 text/html | spring raised MissingServletRequestParameterException
GET /pages/search?q=java&page=abc -> 400 text/html | spring raised MethodArgumentTypeMismatchException
GET /pages/students/abc -> 400 text/html | spring raised MethodArgumentTypeMismatchException
GET /pages/trace -> 200 text/plain | trace=none
GET /pages/trace [X-Trace: abc-123] -> 200 text/plain | trace=abc-123
POST /pages/register [application/x-www-form-urlencoded] name=An&age=20 -> 200 text/plain | registered An aged 20
POST /pages/register [application/x-www-form-urlencoded] name=An&age=abc -> 400 text/html | spring raised MethodArgumentNotValidException
GET /pages/view -> 200 text/html | <html><body><h1>Danh sach sinh vien</h1></body></html>
GET /api/students -> 200 application/json | [{"id":1,"name":"An"},{"id":2,"name":"Binh"}]
GET /api/students/1 -> 200 application/json | {"id":1,"name":"An"}
GET /api/students/9 -> 404
POST /api/students [application/json] {"name":"Chi"} -> 201 application/json Location: /api/students/3 | {"id":3,"name":"Chi"}
POST /api/students [text/plain] Chi -> 415 text/html | spring raised HttpMediaTypeNotSupportedException
GET /api/students/1 [Accept: text/plain] -> 406 text/html | spring raised HttpMediaTypeNotAcceptableException
```

Nếu bạn biên dịch không có cờ `-parameters` (`mvn clean compile exec:exec -Dmaven.compiler.parameters=false ...`), các endpoint dùng `@PathVariable` và `@RequestParam` không ghi tên trả 500 với `IllegalArgumentException: Name for argument of type [int] not specified, and parameter name information not available via reflection. Ensure that the compiler uses the '-parameters' flag.`

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
| `mvn` không tải được `tools.jackson` | Chưa có mạng hoặc kho bị chặn | Chạy lại với mạng |
| Tên tham số không đọc được (500) | Thiếu cờ `-parameters` | Giữ `maven.compiler.parameters=true` trong `pom.xml`, hoặc ghi tên trong annotation |
