# Bài 01 - DispatcherServlet và vòng đời một yêu cầu

Mã nguồn đi kèm bài học **Giới thiệu Spring MVC và DispatcherServlet** (chương 7: Spring MVC) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một ứng dụng Spring MVC trên Tomcat nhúng với `DispatcherServlet`, danh sách các bean chiến lược mà `@EnableWebMvc` đăng ký (`HandlerMapping`, `HandlerAdapter`, `ViewResolver`), thứ tự thật mà một yêu cầu đi qua `Filter`, `HandlerInterceptor`, controller và JSP, và các kết quả 404, 405, 500 cùng việc `afterCompletion` vẫn chạy khi controller ném lỗi.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Framework | 7.0.9 | khai báo trong `pom.xml` |
| Tomcat nhúng | 11.0.27 (Servlet 6.1) | khai báo trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-dispatcherservlet/
  pom.xml
  src/main/java/dev/canhdominich/spring/mvc/
    Trace.java                  ghi lại các sự kiện theo thứ tự xảy ra
    TraceFilter.java            Filter của Servlet ghi trước và sau servlet
    TraceInterceptor.java       HandlerInterceptor ghi preHandle, postHandle, afterCompletion
    HelloController.java        hai endpoint: /hello trả view, /boom ném lỗi
    WebConfig.java              @EnableWebMvc, quét component, interceptor, view resolver JSP
    EmbeddedServer.java         dựng Tomcat, DispatcherServlet, JSP servlet và filter
    ServerMain.java             chạy server thật ở cổng 8080
    DispatcherDemo.java         gửi yêu cầu HTTP thật và in kết quả cùng thứ tự sự kiện
  src/main/webapp/WEB-INF/views/
    hello.jsp                   view của /hello
  src/test/java/dev/canhdominich/spring/mvc/
    DispatcherBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.spring.mvc.DispatcherDemo
```

Để chạy server thật và tự mở bằng trình duyệt:

```bash
mvn -q compile exec:exec
```

rồi mở `http://127.0.0.1:8080/hello`.

## Kết quả mong đợi

Kết quả của `DispatcherDemo` (đã bỏ log của Tomcat và Spring ở stderr):

```text
handler mappings: [beanNameHandlerMapping, requestMappingHandlerMapping, routerFunctionMapping]
handler adapters: [handlerFunctionAdapter, httpRequestHandlerAdapter, requestMappingHandlerAdapter, simpleControllerHandlerAdapter]
view resolvers: [mvcViewResolver]
mapped: {GET [/boom]} -> boom
mapped: {GET [/hello]} -> hello
GET /hello -> 200, body: <html><body><h1>Xin chao Spring MVC</h1></body></html>
    filter: before the servlet
    interceptor: preHandle for hello
    controller: hello()
    interceptor: postHandle, view = hello
    view: hello.jsp rendered
    interceptor: afterCompletion, exception = none
    filter: after the servlet
GET /nope -> 404
    filter: before the servlet
    filter: after the servlet
POST /hello -> 405, Allow: GET
    filter: before the servlet
    filter: after the servlet
GET /boom -> 500
    filter: before the servlet
    interceptor: preHandle for boom
    controller: boom()
    interceptor: afterCompletion, exception = IllegalStateException
    filter: after the servlet
GET /WEB-INF/views/hello.jsp -> 404
```

Log `SEVERE: Servlet.service() ... threw exception` xuất hiện ở stderr cho `/boom`: đó là cách Tomcat ghi lỗi chưa được xử lý, không phải lỗi của lab.

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
| `Address already in use` ở cổng 8080 | Có ứng dụng khác dùng cổng 8080 | `PORT=9090 mvn -q compile exec:exec` |
| Mọi đường dẫn đều 404 dù controller đúng | Thiếu `Tomcat.addContext` hoặc DispatcherServlet chưa được ánh xạ vào `/` | Dùng đúng `EmbeddedServer` của bài (`Tomcat.addContext`); khi tác giả thử `addWebapp` với cùng cách đăng ký, mọi đường dẫn đều trả 404 |
