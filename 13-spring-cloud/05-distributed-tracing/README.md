# Bài 05 - Distributed tracing với Micrometer Tracing

Mã nguồn đi kèm bài học **Distributed tracing với Micrometer Tracing** (chương 13: Spring Cloud) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: ba dịch vụ gọi nhau theo chuỗi `front` rồi `inventory` rồi `pricing`, và quan sát một mã `traceId` đi xuyên cả chuỗi trong header `traceparent`; cây span (mỗi lời gọi có một span phía client và một span phía server, cùng `parentId` nối chúng); việc `traceId` vào ngữ cảnh log (MDC); việc dịch vụ đầu tiếp tục một vết bắt đầu từ bên ngoài; mặc định chỉ khoảng 10 phần trăm vết được ghi lại; và việc luồng mới (như `CompletableFuture`) mất ngữ cảnh. Mọi dịch vụ chạy trong cùng một tiến trình trên cổng ngẫu nhiên.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.0.8 | khai báo ở `parent` trong `pom.xml`; Micrometer Tracing do Boot quản lý |
| Cổng mạng | cổng ngẫu nhiên | ba dịch vụ chạy trong cùng một tiến trình |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
05-distributed-tracing/
  pom.xml                       parent Boot 4.0.8, actuator, RestClient, Micrometer Tracing với Brave
  src/main/java/dev/canhdominich/cloud/
    trace/SpanCollector.java    SpanHandler của Brave: gom các span đã kết thúc để bài đo
    trace/TraceInfo.java        mô tả traceId, spanId, parentId, MDC và header traceparent
    front/FrontApplication.java, FrontController.java          GET /checkout, gọi inventory
    inventory/InventoryApplication.java, InventoryController.java   GET /stock, gọi pricing
    pricing/PricingApplication.java, PricingController.java    GET /price và GET /async
    TracingDemo.java            chạy mọi thí nghiệm
  src/test/java/dev/canhdominich/cloud/
    TracingBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `TracingDemo` (đã bỏ log ở stderr):

```text
-- default sampling
100 requests carried 100 different trace ids, none missing: true
traces actually recorded: 10 of 100
-- every request recorded
services in the answer: [front, inventory, pricing]
same trace id in all three: true
span ids all different: true
front parent: none
header received by inventory carries the same trace id: true
trace id written to the log context (MDC) equals the tracer's: true
spans recorded for the request: [front:CLIENT, front:SERVER, inventory:CLIENT, inventory:SERVER, pricing:SERVER]
spans whose parent is missing from the trace: 0
second request has a different trace id: true
a trace started outside is continued: true
front's parent is the outside span: true
-- crossing a thread
request thread traceId=6ad2f7dbaa87e74e33c0fa4de64e9f3d, other thread traceId=null
-- sampling switched off
trace id still present: true
spans recorded: 0
```

Số `traces actually recorded` thay đổi giữa các lần chạy (khoảng 10 trên 100 vì xác suất lấy mẫu mặc định là 0,1) và giá trị `traceId` ở dòng `request thread` là ngẫu nhiên; mọi dòng khác phải y hệt.

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
| `No qualifying bean of type RestClient$Builder` | Spring Boot 4 tách `RestClient.Builder` ra khỏi starter web | Thêm `spring-boot-starter-restclient` vào `pom.xml` |
| Chương trình không in gì và treo | Một ngữ cảnh khởi động lỗi nhưng log đã tắt bằng `logging.level.root=OFF`, tiến trình còn Tomcat chạy | Bỏ dòng tắt log để thấy lỗi; `TracingDemo` bắt lỗi và in nguyên nhân gốc |
| Không có span nào được ghi | Xác suất lấy mẫu là 0 hoặc không bật theo dõi | Đặt `management.tracing.sampling.probability=1.0` để ghi mọi vết |
