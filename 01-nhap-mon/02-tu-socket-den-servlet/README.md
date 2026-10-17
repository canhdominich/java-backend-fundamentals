# Bài 02 - Từ socket tới servlet: Core Java cho bạn gì, Advanced Java thêm gì

Mã nguồn đi kèm bài học **So sánh Core Java và Advanced Java** (chương 1: Giới thiệu Advanced Java) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: cùng một ứng dụng nhỏ (ba đường dẫn `/hello`, `/slow`, `/agent`) viết hai cách. Cách thứ nhất là một máy chủ HTTP viết tay chỉ bằng Core Java (`ServerSocket`, một vòng lặp, đọc dòng yêu cầu và header). Cách thứ hai là các servlet chạy trong Tomcat nhúng. Bạn đo bốn khác biệt: bốn yêu cầu đồng thời (4 giây so với 1 giây), giải mã chuỗi truy vấn (`An%20Binh` so với `An Binh`), tìm header không phân biệt hoa thường, và mã trạng thái cho một phương thức handler không cài đặt (200 so với 405).

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Tomcat nhúng | 11.0.27 | khai báo trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-tu-socket-den-servlet/
  pom.xml                       tomcat-embed-core 11.0.27, JUnit, AssertJ
  src/main/java/dev/canhdominich/intro/http/
    RawHttpServer.java          máy chủ viết tay trên ServerSocket, một luồng
    Servlets.java               HelloServlet, SlowServlet, AgentServlet
    ServletServer.java          Tomcat nhúng đăng ký ba servlet
    CompareDemo.java            chạy hai máy chủ và so sánh
  src/test/java/dev/canhdominich/intro/http/
    CompareBehaviourTest.java   năm test
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec 2>/dev/null
```

## Kết quả mong đợi

Kết quả của `CompareDemo` (đã bỏ log của Tomcat ở stderr):

```text
-- four requests at the same time, each takes 1 s on the server
hand-written server on a socket: all four done after about 4.0 s
servlet container: all four done after about 1.0 s
-- GET /hello?name=An%20Binh
hand-written server answers: hello An%20Binh
servlet container answers: hello An Binh
-- reading the header User-Agent: lab as "user-agent"
hand-written server: null
servlet container: lab
-- POST to a path whose handler only knows GET
hand-written server: status 200
servlet container: status 405
-- a path nobody handles
hand-written server: status 404
servlet container: status 404
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
| Chương trình in nhiều dòng `INFO` của Tomcat | Tomcat ghi log ra stderr | Chạy với `2>/dev/null` hoặc đọc dòng bắt đầu bằng `--` |
| Số giây khác bài (ví dụ 5.0 thay vì 4.0) | Máy chậm hoặc đang bận | Chạy lại; điều cố định là máy chủ viết tay xếp hàng còn Tomcat chạy song song |
