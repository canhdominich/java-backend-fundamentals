# Bài 02 - Vòng đời của Servlet

Mã nguồn đi kèm bài học **Vòng đời của Servlet** (chương 3: Servlet) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: thời điểm `init()` và `destroy()` được gọi (servlet nạp sớm và nạp muộn), thứ container làm khi `init()` ném lỗi, hai phương thức HTTP mà container tự thêm (`OPTIONS` và `HEAD`), lỗi cập nhật bị mất khi nhiều luồng cùng sửa một trường của servlet và hai cách sửa, và lý do biến static trong ứng dụng web không dùng chung được với chương trình chạy server.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Apache Tomcat Embed | 11.0.27 | khai báo trong `pom.xml`, hiện thực Jakarta Servlet 6.1 |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-vong-doi-servlet/
  pom.xml
  src/main/webapp/WEB-INF/web.xml       deployment descriptor tối thiểu (Servlet 6.1)
  src/main/java/dev/canhdominich/servlet/lifecycle/
    EmbeddedServer.java       dựng Tomcat nhúng, quét lớp trong target/classes
    ServerMain.java           chạy server ở cổng 8080 (đổi bằng biến PORT)
    EventLog.java             ghi và in các sự kiện vòng đời
    EventsServlet.java        trả danh sách sự kiện qua HTTP (/events)
    LifecycleServlets.java    EagerServlet (loadOnStartup=1) và LazyServlet
    CounterServlet.java       ba bộ đếm: không an toàn luồng, synchronized, AtomicInteger
    FailingInitServlet.java   hai servlet có init() ném lỗi
    MethodsServlet.java       servlet chỉ có doGet và doPost, để xem OPTIONS và HEAD
    LifecycleProbes.java      chạy toàn bộ thí nghiệm và in kết quả
  src/test/java/dev/canhdominich/servlet/lifecycle/
    LifecycleBehaviourTest.java
```

## Chạy server

```bash
mvn -q compile exec:exec
```

Server lắng nghe ở `http://localhost:8080`. Thử:

```bash
curl -i http://localhost:8080/life/lazy
curl -i -X OPTIONS http://localhost:8080/methods
curl -I http://localhost:8080/methods
curl http://localhost:8080/events
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.servlet.lifecycle.LifecycleProbes
```

Các dòng `event:` do chính servlet in ra khi container gọi `init`, `doGet`, `destroy`. Các dòng log của Tomcat có ngày giờ và `SEVERE` khi `init()` ném lỗi là bình thường.

## Kết quả mong đợi

```text
-- starting the server
  event: EagerServlet.init() on main
-- server started; requests to the lazy servlet twice, then the eager servlet once
  event: LazyServlet.init() on http-nio-127.0.0.1-auto-1-exec-N
  event: LazyServlet.doGet()
  event: LazyServlet.doGet()
  event: EagerServlet.doGet()
-- failing init
  ordinary exception, request 1: 500
  ordinary exception, request 2: 200 ready after 2 init attempts
  UnavailableException, request 1: 404
  UnavailableException, request 2: 404
-- methods the container adds
  OPTIONS /methods -> 200 Allow=GET, HEAD, POST, OPTIONS
  HEAD /methods    -> 200 Content-Length=17 body length=0
-- 2000 increments from 16 client threads
  unsafe       final value 912
  synchronized final value 2000
  atomic       final value 2000
-- stopping the server
  event: LazyServlet.destroy()
  event: EagerServlet.destroy()
```

Giá trị của `unsafe` thay đổi mỗi lần chạy nhưng luôn nhỏ hơn 2000 (hai lần đo cho 898 và 912). Phương thức `incrementUnsafe` cố ý gọi `Thread.yield()` giữa lúc đọc và lúc ghi để lỗi lộ ra gần như chắc chắn; không có dòng đó lỗi vẫn có nhưng chỉ lộ ra ở tải cao.

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
| `/events` trả rỗng | Chưa có servlet nào được gọi | Gọi `/life/eager` hoặc `/life/lazy` trước |
| `unsafe` ra đúng 2000 | Máy quá nhanh hoặc ít lõi nên không có tranh chấp | Tăng `INCREMENTS` hoặc số luồng client trong `LifecycleProbes` |
