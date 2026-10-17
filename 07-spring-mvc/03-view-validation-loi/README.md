# Bài 03 - View, validation và xử lý lỗi

Mã nguồn đi kèm bài học **View, Validation và Exception Handling** (chương 7: Spring MVC) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một form đăng ký với Bean Validation (thông báo lỗi theo từng trường, form giữ lại giá trị), mẫu Post/Redirect/Get so với trả view ngay sau POST (đếm số lần lưu khi trình duyệt gửi lại), flash attribute chỉ sống một yêu cầu, `@Valid` thiếu `BindingResult`, và xử lý lỗi tập trung: `@ControllerAdvice` trả trang lỗi HTML, `@RestControllerAdvice` trả JSON kể cả lỗi kiểm tra dữ liệu và lỗi bất ngờ không lộ thông điệp, và `@ExceptionHandler` cục bộ thắng advice.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Framework | 7.0.9 | khai báo trong `pom.xml` |
| Hibernate Validator | 9.1.5.Final | cài đặt Bean Validation |
| Tomcat nhúng | 11.0.27 | gồm `tomcat-embed-el` cho thông báo kiểm tra dữ liệu |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
03-view-validation-loi/
  pom.xml
  src/main/java/dev/canhdominich/spring/mvc/
    Trace.java, EmbeddedServer.java, ServerMain.java   như bài trước
    User.java                   dữ liệu form với @NotBlank, @Email, @Min
    UserStore.java              nơi lưu tạm tên người dùng để đếm số lần lưu
    RegisterController.java     form, submit có kiểm tra, redirect, bản không redirect, bản thiếu BindingResult
    CourseNotFoundException.java ngoại lệ nghiệp vụ
    CourseController.java       trang HTML ném CourseNotFoundException
    LocalHandlingController.java có @ExceptionHandler cục bộ
    CourseApi.java              REST: ném lỗi, @Valid @RequestBody, lỗi bất ngờ
    PageAdvice.java             @ControllerAdvice cho các trang: trả view lỗi 404
    ApiAdvice.java              @RestControllerAdvice cho API: trả JSON cho 404, 400, 500
    WebConfig.java              @EnableWebMvc và view resolver
    FormDemo.java               gửi các yêu cầu bằng HttpClient có cookie và in kết quả
  src/main/webapp/WEB-INF/views/
    register.jsp, registered.jsp, error.jsp
  src/test/java/dev/canhdominich/spring/mvc/
    ValidationAndErrorsTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.spring.mvc.FormDemo
```

Để chạy server thật và dùng trình duyệt: `mvn -q compile exec:exec` rồi mở `http://127.0.0.1:8080/register`.

## Kết quả mong đợi

Kết quả của `FormDemo` (đã bỏ log ở stderr). Số `count` của dòng `register-no-redirect` bắt đầu từ 2 vì người dùng "An" đã được lưu ở bước trước:

```text
GET /register -> 200 | form with inputs for name, email, age
POST /register name=&email=x&age=10 -> 200 | errors [name: must not be blank, email: must be a well-formed email address, age: must be greater than or equal to 18]
POST /register name=An&email=an%40example.com&age=20 -> 302 Location: /registered
GET /registered -> 200 | message=[Saved An] count=[1]
GET /registered -> 200 | message=[] count=[1]
POST /register-no-redirect name=Binh&email=binh%40example.com&age=30 -> 200 | message=[Saved Binh] count=[2]
POST /register-no-redirect name=Binh&email=binh%40example.com&age=30 -> 200 | message=[Saved Binh] count=[3]
POST /register-strict name=&email=x&age=10 -> 400
GET /pages/course/1 -> 200 | course 1
GET /pages/course/9 -> 404 | error=[Course 9 does not exist]
GET /local/course/9 -> 200 | handled inside the controller: Course 9 does not exist
GET /api/course/9 -> 404 | {"status":404,"error":"Course 9 does not exist"}
POST /api/users {"name":"","email":"x","age":10} -> 400 | {"status":400,"error":"Validation failed","fields":{"age":"must be greater than or equal to 18","email":"must be a well-formed email address","name":"must not be blank"}}
GET /api/boom -> 500 | {"status":500,"error":"Internal server error"}
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
| Không thấy thông báo lỗi trên form | Thiếu Hibernate Validator hoặc EL trên classpath | Giữ `hibernate-validator` và `tomcat-embed-el` trong `pom.xml` |
| Form báo `Neither BindingResult nor plain target object` | Quên đặt `user` vào Model ở GET | Giữ `model.addAttribute("user", new User())` |
