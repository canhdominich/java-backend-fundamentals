# Bài 03 - Request, Response và Session

Mã nguồn đi kèm bài học **Request, Response và Session** (chương 3: Servlet) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: đọc tham số, tiêu đề và đường dẫn từ yêu cầu, mã hóa ký tự mặc định của yêu cầu và phản hồi (và vì sao tiếng Việt hỏng nếu không đặt), thân yêu cầu chỉ đọc được một lần, các cách trả phản hồi (`setStatus`, `sendError`, `sendRedirect`), phản hồi đã commit, thuộc tính cookie, và vòng đời của `HttpSession` kể cả tấn công session fixation và cách chặn.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Apache Tomcat Embed | 11.0.27 | khai báo trong `pom.xml`, hiện thực Jakarta Servlet 6.1 |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
03-request-response-session/
  pom.xml
  src/main/webapp/WEB-INF/web.xml       deployment descriptor tối thiểu (Servlet 6.1)
  src/main/java/dev/canhdominich/servlet/reqresp/
    EmbeddedServer.java       dựng Tomcat nhúng, quét lớp trong target/classes
    ServerMain.java           chạy server ở cổng 8080 (đổi bằng biến PORT)
    EchoServlet.java          in lại mọi thứ servlet đọc được từ yêu cầu
    EncodingServlet.java      mã hóa ký tự của phản hồi và của thân yêu cầu
    BodyServlet.java          thân yêu cầu là luồng đọc một lần; thân JSON
    ResponseServlets.java     setStatus, sendError, sendRedirect, phản hồi đã commit, cookie
    SessionServlets.java      tạo session, đăng nhập, whoami, đăng xuất, hết hạn
    RequestResponseProbes.java chạy toàn bộ thí nghiệm và in kết quả
  src/test/java/dev/canhdominich/servlet/reqresp/
    RequestResponseBehaviourTest.java
```

## Chạy server

```bash
mvn -q compile exec:exec
```

Server lắng nghe ở `http://localhost:8080`. Thử:

```bash
curl -i "http://localhost:8080/echo?name=An&hobby=code&hobby=music"
curl -i -X POST -d "ten=An" http://localhost:8080/body/parameter-first
curl -i -c jar.txt http://localhost:8080/session/start
curl -i -b jar.txt http://localhost:8080/session/start
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec -Dmain.class=dev.canhdominich.servlet.reqresp.RequestResponseProbes
```

Chương trình mất khoảng 5 giây vì có một bước chờ session hết hạn. Các dòng log của Tomcat có ngày giờ là bình thường.

## Kết quả mong đợi

Giá trị `Expires` của cookie và mã session thay đổi mỗi lần chạy nên không in ở đây. Trong dòng `ten = ...` của yêu cầu bị giải mã sai, giữa `Nguyá»` và `n` có một ký tự điều khiển không nhìn thấy (U+0085).

```text
-- reading the request: GET with query string
method        = GET
requestURI    = /echo
contextPath   = ''
servletPath   = /echo
queryString   = name=An&hobby=code&hobby=music
name          = An
hobby         = [code, music]
missing       = null
User-Agent    = lab-client
contentType   = null
remoteAddr    = 127.0.0.1

-- POST form: query string and body both carry name
method        = POST
requestURI    = /echo
contextPath   = ''
servletPath   = /echo
queryString   = name=fromQuery
name          = fromQuery
hobby         = [a]
missing       = null
User-Agent    = Java-http-client/17.0.15
contentType   = application/x-www-form-urlencoded
remoteAddr    = 127.0.0.1

-- response without and with charset
/encoding/response-default Content-Type=none as UTF-8: Xin ch�o Vi?t Nam | as ISO-8859-1: Xin chào Vi?t Nam
/encoding/response-utf8 Content-Type=text/plain;charset=UTF-8 as UTF-8: Xin chào Việt Nam | as ISO-8859-1: Xin chÃ o Viá»t Nam
-- request body encoding, form field ten=Nguyễn sent as UTF-8
no setCharacterEncoding        : declared encoding = null | ten = Nguyá»n
setCharacterEncoding(UTF-8) first : declared encoding = UTF-8 | ten = Nguyễn
setCharacterEncoding(ISO-8859-1) first : declared encoding = ISO-8859-1 | ten = Nguyá»n
setCharacterEncoding(ISO-8859-1) after getParameter : declared encoding = ISO-8859-1 | ten = Nguyá»n
-- the request body is a one-shot stream
read body, then getParameter : body length = 6 | ten after reading the body = null
getParameter, then read body : ten = An | body length after getParameter = 0
JSON body                    : 201 {"received":15,"contentType":"application/json"}
-- status, error, redirect, committed response, cookie
setStatus(201)   : 201 Location=/orders/42
sendError(404)   : 404 body mentions the message: true, mentions Apache Tomcat: true
sendRedirect     : 302 Location=/echo?name=after-redirect
response committed:
bufferSize=8192 committedBefore=false
committedAfter=true IllegalStateException: Cannot call sendRedirect() after the response has been committed
Set-Cookie       : theme=dark; Max-Age=604800; Path=/; Secure; HttpOnly; SameSite=Lax
sending it back  : cookies received: theme=dark
-- sessions
first request   : id=<id> new=true maxInactiveInterval=1800
Set-Cookie      : JSESSIONID=<id>; Path=/; HttpOnly
with the cookie : id=<id> new=false maxInactiveInterval=1800 Set-Cookie sent again: false
whoami, no cookie : anonymous (no session)
-- session fixation: the attacker plants a session id, the victim logs in
no id change on login    : id changed=false, attacker using the planted id sees: user=victim
changeSessionId() on login : id changed=true, attacker using the planted id sees: anonymous (no session)
-- logout and timeout
after login  : user=an
after logout : anonymous (no session)
timeout 2 s, immediately : user=an
timeout 2 s, after 3.5 s: anonymous (no session)
```

## Chạy bộ test

```bash
mvn test
```

Kết quả mong đợi: `Tests run: 9, Failures: 0, Errors: 0`.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Address already in use` ở cổng 8080 | Có ứng dụng khác dùng cổng 8080 | `PORT=9090 mvn -q compile exec:exec` |
| Tiếng Việt thành `?` hoặc ký tự lạ | Thiếu `charset=UTF-8` ở phản hồi, hoặc thiếu `setCharacterEncoding("UTF-8")` ở yêu cầu | Đặt như `EncodingServlet.ResponseUtf8` và `X-Force-Charset: UTF-8` trong thí nghiệm |
| Test session hết hạn thỉnh thoảng đỏ | Máy rất chậm nên chưa tới hạn | Tăng thời gian chờ trong `logoutAndTimeoutEndTheSession` |
