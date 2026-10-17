# Bài 01 - Giới thiệu và kiến trúc Spring Boot

Mã nguồn đi kèm bài học **Giới thiệu và kiến trúc Spring Boot** (chương 8: Spring Boot) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một ứng dụng Spring Boot tối thiểu với một controller, bản kiểm kê những gì Boot tự cấu hình (phiên bản Spring Boot, Spring Framework và Tomcat, các auto-configuration đã áp dụng và không áp dụng, các bean xử lý yêu cầu), một yêu cầu `GET /hello` và một lỗi 404 có thân JSON, và đóng gói thành executable jar rồi chạy bằng `java -jar`.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml`; quản lý Spring Framework 7.0.9 và Tomcat 11.0.24 |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-gioi-thieu-kien-truc/
  pom.xml                       parent spring-boot-starter-parent 4.1.1, hai starter
  src/main/java/dev/canhdominich/boot/tour/
    Application.java            @SpringBootApplication và main
    HelloController.java        @RestController với GET /hello
    TourDemo.java               khởi động ứng dụng và in bản kiểm kê
  src/test/java/dev/canhdominich/boot/tour/
    TourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:java -Dexec.mainClass=dev.canhdominich.boot.tour.TourDemo
```

Chạy ứng dụng như một server thật và đóng gói:

```bash
mvn -q spring-boot:run
mvn -q package -DskipTests
java -jar target/boot-tour-1.0.0.jar --server.port=8080
curl http://localhost:8080/hello
```

## Kết quả mong đợi

Kết quả của `TourDemo` (đã bỏ log ở stderr; dòng danh sách auto-configuration dài nên được rút gọn bằng dấu ba chấm ở bài học):

```text
spring boot 4.1.1, spring framework 7.0.9, tomcat 11.0.24.0
beans in the context: more than 100, declared by this project's classes: [application, helloController]
auto-configurations that applied: 11 [AopAutoConfiguration, DispatcherServletAutoConfiguration, ErrorMvcAutoConfiguration, HttpEncodingAutoConfiguration, HttpMessageConvertersAutoConfiguration, JacksonAutoConfiguration, MultipartAutoConfiguration, TaskExecutionAutoConfiguration, TaskSchedulingAutoConfiguration, TomcatServletWebServerAutoConfiguration, WebMvcAutoConfiguration]
  DispatcherServletAutoConfiguration: applied
  TomcatServletWebServerAutoConfiguration: applied
  WebMvcAutoConfiguration: applied
  JacksonAutoConfiguration: applied
  HttpMessageConvertersAutoConfiguration: applied
  ErrorMvcAutoConfiguration: applied
auto-configurations whose conditions did not match: 13 [JmxAutoConfiguration, MessageSourceAutoConfiguration, ServletHttpExchangesAutoConfiguration, ServletManagementContextAutoConfiguration, ServletMappingsAutoConfiguration, SpringApplicationAdminJmxAutoConfiguration, TomcatMetricsAutoConfiguration, TomcatReactiveManagementContextAutoConfiguration, TomcatReactiveWebServerAutoConfiguration, TomcatServletManagementContextAutoConfiguration, WebMvcHealthEndpointExtensionAutoConfiguration, WebMvcMappingsAutoConfiguration, WebMvcObservationAutoConfiguration]
handler mappings: [beanNameHandlerMapping, requestMappingHandlerMapping, resourceHandlerMapping, routerFunctionMapping, welcomePageHandlerMapping, welcomePageNotAcceptableHandlerMapping]
embedded server listening on a port chosen by the OS: true
GET /hello -> 200 application/json {"message":"Xin chao Spring Boot"}
GET /nope -> 404 application/json {"status":404,"error":"Not Found","path":"/nope"}
startup under 20 seconds: true
```

Executable jar (đo trên máy tác giả): kích thước khoảng 19,9 MB, 34 jar trong `BOOT-INF/lib`, `Main-Class: org.springframework.boot.loader.launch.JarLauncher`, `Start-Class: dev.canhdominich.boot.tour.Application`, `Spring-Boot-Version: 4.1.1`, và dòng log `Started Application in 3.165 seconds`.

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
| `mvn` không tải được thư viện Spring Boot | Chưa có mạng | Chạy lại với mạng |
| `Port 8080 was already in use` | Cổng 8080 đang bị chiếm | `java -jar target/boot-tour-1.0.0.jar --server.port=8081` |
