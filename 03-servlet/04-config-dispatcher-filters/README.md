# Bài 04 - Config, Dispatcher và Filters

Mã nguồn đi kèm bài học **Config, Dispatcher và Filters** (chương 3: Servlet) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: tham số khởi tạo của một servlet (`ServletConfig`) so với tham số của cả ứng dụng (`ServletContext`), listener của ứng dụng và của session, `forward`, `include` và `sendRedirect` tới cùng một đích, chuỗi filter (thứ tự, ngắt chuỗi, loại điều phối), một request wrapper cho phép đọc thân hai lần, và một front controller tự viết (ý tưởng của `DispatcherServlet`).

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Apache Tomcat Embed | 11.0.27 | khai báo trong `pom.xml`, hiện thực Jakarta Servlet 6.1 |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
04-config-dispatcher-filters/
  pom.xml
  src/main/webapp/WEB-INF/web.xml       context-param, bốn filter và thứ tự của chúng
  src/main/java/dev/canhdominich/servlet/filters/
    EmbeddedServer.java       dựng Tomcat nhúng, quét lớp trong target/classes
    ServerMain.java           chạy server ở cổng 8080 (đổi bằng biến PORT)
    EventLog.java             in các sự kiện của filter, servlet, listener
    AppListener.java          ServletContextListener và HttpSessionListener
    ConfigServlet.java        đọc init-param của servlet và context-param của ứng dụng
    DispatchServlets.java     forward, include, redirect tới cùng một servlet đích
    LoggingFilter.java        ghi trước và sau chuỗi
    EncodingFilter.java       đặt UTF-8 cho mọi yêu cầu
    AuthFilter.java           chặn /secure/* nếu thiếu tiêu đề X-Token
    ForwardLoggingFilter.java filter được ánh xạ cho REQUEST, FORWARD, INCLUDE
    AnnotatedFilters.java     hai filter khai báo bằng annotation
    BodyCachingFilter.java    đọc thân một lần và bọc yêu cầu
    ApiServlet.java           đọc thân hai lần nhờ wrapper
    SecureServlet.java        /secure/data và /annotated
    FrontControllerServlet.java front controller tự viết ở /app/*
    StatusServlet.java        servlet đăng ký bằng web.xml và bằng mã
    FilterProbes.java         chạy toàn bộ thí nghiệm và in kết quả
  src/test/java/dev/canhdominich/servlet/filters/
    FilterBehaviourTest.java
```

## Chạy server

```bash
mvn -q compile exec:exec
```

Server lắng nghe ở `http://localhost:8080`. Thử:

```bash
curl -i http://localhost:8080/config
curl -i http://localhost:8080/secure/data
curl -i -H "X-Token: secret" http://localhost:8080/secure/data
curl -i http://localhost:8080/dispatch/forward
curl -i http://localhost:8080/app/orders/42
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.servlet.filters.FilterProbes
```

Các dòng `event:` do filter, servlet và listener tự in ra, theo đúng thứ tự container gọi chúng.

## Kết quả mong đợi

```text
-- starting the server
  event: context initialized for Course Shop
-- ServletConfig and ServletContext
  event: logging filter: before /config
  event: encoding filter: UTF-8 set before the servlet runs
  event: logging filter: after /config, status 200
ServletConfig  greeting      = Xin chào
ServletConfig  appName       = null
ServletContext appName       = Course Shop
ServletContext greeting      = null
ServletContext startedBy     = AppListener for Course Shop
ServletContext contextPath   = ''
ServletContext serverInfo    = Apache Tomcat/11.0.27
-- servlets registered in web.xml and in code (the annotation way is ConfigServlet above)
  event: logging filter: before /health
  event: encoding filter: UTF-8 set before the servlet runs
  event: logging filter: after /health, status 200
status servlet registered by web.xml at /health
  event: logging filter: before /programmatic
  event: encoding filter: UTF-8 set before the servlet runs
  event: logging filter: after /programmatic, status 200
status servlet registered by ServletContext.addServlet at /programmatic
-- forward, include and redirect to the same target
  event: logging filter: before /dispatch/forward
  event: encoding filter: UTF-8 set before the servlet runs
  event: forwardLogging filter: dispatcher type FORWARD
  event: logging filter: after /dispatch/forward, status 202
forward : status 202, header X-Set-By-Target=yes
    target: getRequestURI    = /dispatch/target
    target: forward original = /dispatch/forward
    target: include uri attr = null
    target: message attribute = set before forward
  event: logging filter: before /dispatch/include
  event: encoding filter: UTF-8 set before the servlet runs
  event: forwardLogging filter: dispatcher type INCLUDE
  event: logging filter: after /dispatch/include, status 200
include : status 200, header X-Set-By-Target=absent
    --- before include (written by the including servlet)
    target: getRequestURI    = /dispatch/include
    target: forward original = null
    target: include uri attr = /dispatch/target
    target: message attribute = set before include
    --- after include (written by the including servlet)
  event: logging filter: before /dispatch/redirect
  event: encoding filter: UTF-8 set before the servlet runs
  event: logging filter: after /dispatch/redirect, status 302
redirect: first response 302 Location=/dispatch/target
  event: logging filter: before /dispatch/redirect
  event: encoding filter: UTF-8 set before the servlet runs
  event: logging filter: after /dispatch/redirect, status 302
  event: logging filter: before /dispatch/target
  event: encoding filter: UTF-8 set before the servlet runs
  event: forwardLogging filter: dispatcher type REQUEST
  event: logging filter: after /dispatch/target, status 202
redirect followed: status 202, header X-Set-By-Target=yes
    target: getRequestURI    = /dispatch/target
    target: forward original = null
    target: include uri attr = null
    target: message attribute = null
-- filter chain around a normal request
  event: logging filter: before /annotated
  event: encoding filter: UTF-8 set before the servlet runs
  event: annotated filter apple
  event: annotated filter zebra
  event: annotated servlet runs
  event: logging filter: after /annotated, status 200
-- auth filter without a token, then with one
  event: logging filter: before /secure/data
  event: encoding filter: UTF-8 set before the servlet runs
  event: auth filter: rejected, chain not continued
  event: logging filter: after /secure/data, status 401
  status 401
  event: logging filter: before /secure/data
  event: encoding filter: UTF-8 set before the servlet runs
  event: auth filter: token accepted
  event: secure servlet runs
  event: logging filter: after /secure/data, status 200
  status 200 body secret data
-- dispatcher types seen by the filter mapped to FORWARD and INCLUDE
  event: logging filter: before /dispatch/target
  event: encoding filter: UTF-8 set before the servlet runs
  event: forwardLogging filter: dispatcher type REQUEST
  event: logging filter: after /dispatch/target, status 202
  event: logging filter: before /dispatch/forward
  event: encoding filter: UTF-8 set before the servlet runs
  event: forwardLogging filter: dispatcher type FORWARD
  event: logging filter: after /dispatch/forward, status 202
  event: logging filter: before /dispatch/include
  event: encoding filter: UTF-8 set before the servlet runs
  event: forwardLogging filter: dispatcher type INCLUDE
  event: logging filter: after /dispatch/include, status 200
-- request wrapper that caches the body
  event: logging filter: before /api/echo
  event: encoding filter: UTF-8 set before the servlet runs
  event: body caching filter: read 15 bytes once
  event: logging filter: after /api/echo, status 200
servlet read 15 characters, a second read gave 15
wrapper class seen by the servlet: CachedBodyRequest
-- front controller on /app/*
  event: logging filter: before /app/orders
  event: encoding filter: UTF-8 set before the servlet runs
  event: logging filter: after /app/orders, status 200
/app/orders -> 200 front controller routed /orders -> order list
  event: logging filter: before /app/orders/42
  event: encoding filter: UTF-8 set before the servlet runs
  event: logging filter: after /app/orders/42, status 200
/app/orders/42 -> 200 front controller routed /orders/42 -> order 42
  event: logging filter: before /app/health
  event: encoding filter: UTF-8 set before the servlet runs
  event: logging filter: after /app/health, status 200
/app/health -> 200 front controller routed /health -> up
  event: logging filter: before /app/unknown
  event: encoding filter: UTF-8 set before the servlet runs
  event: logging filter: after /app/unknown, status 404
/app/unknown -> 404 no handler for /unknown
-- session listener
  event: logging filter: before /config
  event: encoding filter: UTF-8 set before the servlet runs
  event: session created, active=1
  event: logging filter: after /config, status 200
  event: logging filter: before /config
  event: encoding filter: UTF-8 set before the servlet runs
  event: session destroyed, active=0
  event: logging filter: after /config, status 200
-- stopping the server
  event: context destroyed
```

Thứ tự `apple` trước `zebra` của hai filter dùng annotation là thứ tự bảng chữ cái của tên filter trên Tomcat 11.0.27; đặc tả không bảo đảm điều đó.

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 11, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Address already in use` ở cổng 8080 | Có ứng dụng khác dùng cổng 8080 | `PORT=9090 mvn -q compile exec:exec` |
| Filter khai báo trong `web.xml` không chạy | Sai tên lớp trong `filter-class` hoặc sai `url-pattern` | Kiểm tra log lúc khởi động và tên lớp đầy đủ |
| `/secure/data` luôn 401 | Thiếu tiêu đề `X-Token: secret` | `curl -H "X-Token: secret" ...` |
