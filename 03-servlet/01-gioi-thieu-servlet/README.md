# Bài 01 - Giới thiệu Servlet và cách hoạt động

Mã nguồn đi kèm bài học **Giới thiệu Servlet và cách hoạt động** (chương 3: Servlet) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một servlet container (Tomcat 11 nhúng) trong một lớp `main`, `@WebServlet` được quét tự động, quy tắc ánh xạ URL (chính xác, tiền tố, đuôi tệp, servlet mặc định), một instance servlet phục vụ nhiều luồng, trang lỗi 404, 405 và 500 mặc định (cùng chỗ nó làm lộ thông tin), cách tắt, và trao đổi HTTP thô qua socket.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Apache Tomcat Embed | 11.0.27 | khai báo trong `pom.xml`, hiện thực Jakarta Servlet 6.1 |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-gioi-thieu-servlet/
  pom.xml
  src/main/webapp/WEB-INF/web.xml       deployment descriptor tối thiểu (Servlet 6.1)
  src/main/java/dev/canhdominich/servlet/intro/
    EmbeddedServer.java       dựng Tomcat nhúng, quét lớp trong target/classes, tùy chọn lab.hardenErrors
    ServerMain.java           chạy server ở cổng 8080 (đổi bằng biến PORT)
    HelloServlet.java         servlet đầu tiên, trả HTML UTF-8
    MappingServlets.java      bốn servlet minh họa quy tắc ánh xạ URL
    ShadowDefaultListener.java đăng ký servlet "/" khi bật lab.shadowDefault
    InstanceServlet.java      cho biết instance, luồng, số lần init()
    BoomServlet.java          ném ngoại lệ để xem trang lỗi 500
    RawHttp.java              gửi và nhận HTTP thô qua socket
    HttpProbes.java           chạy tất cả thí nghiệm và in kết quả
  src/test/java/dev/canhdominich/servlet/intro/
    ServletBehaviourTest.java, HardenedErrorsTest.java
```

## Chạy server

```bash
mvn -q compile exec:exec
```

Mặc định lớp chạy là `ServerMain`, lắng nghe ở `http://localhost:8080`. Thử:

```bash
curl -i "http://localhost:8080/hello?name=An"
curl -i http://localhost:8080/orders/42
curl -i -X POST http://localhost:8080/hello
curl -i http://localhost:8080/boom
```

Dừng server bằng Ctrl+C. Đổi cổng: `PORT=9090 mvn -q compile exec:exec`.

## Chạy từng thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.servlet.intro.RawHttp
mvn -q exec:exec -Dmain.class=dev.canhdominich.servlet.intro.HttpProbes
mvn -q exec:exec -Dmain.class=dev.canhdominich.servlet.intro.HttpProbes -Dmain.arg=harden
mvn -q exec:exec -Dmain.class=dev.canhdominich.servlet.intro.HttpProbes -Dmain.arg=shadow
```

Các dòng log của Tomcat có ngày giờ và `SEVERE` khi servlet ném lỗi là bình thường, vì `BoomServlet` cố ý ném ngoại lệ.

## Kết quả mong đợi

`RawHttp`:

```text
HTTP/1.1 200 
Content-Type: text/html;charset=UTF-8
Content-Length: 33
Date: ...
Connection: close

<h1>Xin chào từ Servlet!</h1>
```

`HttpProbes` (không đối số):

```text
/orders            -> ExactServlet servletPath=/orders pathInfo=null
/orders/42         -> PrefixServlet servletPath=/orders pathInfo=/42
/orders/42/items   -> PrefixServlet servletPath=/orders pathInfo=/42/items
/report.do         -> ExtensionServlet servletPath=/report.do pathInfo=null
/orders/x.do       -> PrefixServlet servletPath=/orders pathInfo=/x.do
/other             -> 404 Tomcat error page, server version in body: true, length 753
POST /hello        -> 405 Allow=none
GET /boom          -> 500 exception class in body: true, secret in body: true, server version in body: true, body length 1108
40 parallel requests -> distinct servlet instances=1, distinct worker threads>1: true, init() calls=1
connector maxThreads=200 minSpareThreads=10
```

Với `harden`: các trang 404 và 500 không còn chứa phiên bản Tomcat, tên ngoại lệ hay nội dung thông báo (`GET /boom` có `body length 451`). Với `shadow`: `/other`, `/index.html` và `/nowhere/page` đều do `ShadowDefaultServlet` trả lời thay vì 404.

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 8, Failures: 0, Errors: 0`. Test chạy server trên cổng ngẫu nhiên nên không cần giải phóng cổng 8080.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Address already in use` ở cổng 8080 | Có ứng dụng khác dùng cổng 8080 | `PORT=9090 mvn -q compile exec:exec` |
| Các lớp servlet không được nhận, mọi URL trả 404 | Chưa biên dịch nên `target/classes` rỗng | Chạy `mvn compile` (lệnh trên đã gồm `compile`) |
| `ClassNotFoundException: org.apache.jasper.servlet.JspServlet` trong log | Thiếu `tomcat-embed-jasper` | Giữ nguyên dependency trong `pom.xml`; Tomcat khai báo servlet `jsp` trong cấu hình mặc định |
| Tiếng Việt hiện sai trên trình duyệt | Thiếu `charset=UTF-8` trong `setContentType` | Dùng `text/html;charset=UTF-8` như `HelloServlet` |
