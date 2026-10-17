# Bài 01 - Service discovery với Eureka và chia tải phía client

Mã nguồn đi kèm bài học **Service discovery với Eureka** (chương 13: Spring Cloud) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một sổ đăng ký Eureka, hai bản của dịch vụ `catalog` tự đăng ký, và dịch vụ `shop` gọi `http://catalog/whoami` mà không biết địa chỉ nào cả (Spring Cloud LoadBalancer chọn bản theo vòng tròn nên 20 lời gọi chia 10 và 10). Sau đó bạn thấy hai cách một bản biến mất: tắt êm (SIGTERM, bản tự hủy đăng ký nên mọi thứ gọn trong vài giây) và sập đột ngột (SIGKILL, sổ đăng ký vẫn liệt kê bản đã chết cho đến khi hợp đồng thuê hết hạn, trong lúc đó một phần lời gọi lỗi `Connection refused`). Mỗi dịch vụ chạy trong một tiến trình Java riêng để mô phỏng đúng thực tế.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.0.8 | khai báo ở `parent` trong `pom.xml`; Spring Cloud 2025.1.3 khớp với dòng Boot 4.0 |
| Cổng mạng | cổng ngẫu nhiên | mỗi tiến trình tự xin cổng trống nên không đụng cổng 8080 hay 8761 |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
01-service-discovery/
  pom.xml                       parent Boot 4.0.8, BOM Spring Cloud 2025.1.3, Eureka server và client, LoadBalancer
  src/main/java/dev/canhdominich/cloud/
    registry/RegistryApplication.java    @EnableEurekaServer: sổ đăng ký
    catalog/CatalogApplication.java      dịch vụ được tra cứu, tự đăng ký
    catalog/CatalogController.java       GET /whoami trả về instance-id của bản đang chạy
    shop/ShopApplication.java            hai RestClient.Builder: một thường (@Primary) và một @LoadBalanced
    shop/ShopController.java             GET /call gọi http://catalog/whoami, GET /instances đếm bản thấy được
    DiscoveryDemo.java                   mở ba loại tiến trình, đo từng tình huống
  src/test/java/dev/canhdominich/cloud/
    DiscoveryBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

Chương trình mất khoảng một phút vì nó chờ các tiến trình khởi động và chờ hợp đồng thuê hết hạn. Nhật ký của từng tiến trình nằm ở `target/logs/`.

## Kết quả mong đợi

Kết quả của `DiscoveryDemo`:

```text
-- a registry with nothing registered
catalog instances in the registry: []
-- two instances of one service register themselves
shop sees 2 instances of catalog within 40 s: true
instances listed by the registry: [catalog-A, catalog-B]
-- the load balancer spreads calls
20 calls to http://catalog/whoami: {catalog-A=10, catalog-B=10}
-- an instance leaves cleanly (SIGTERM)
registry lists only catalog-B within 15 s: true
shop sees 1 instance within 15 s: true
10 calls afterwards: {catalog-B=10}
-- an instance that crashes (SIGKILL)
some calls failed while the registry still listed the dead instance: true
failed calls: 31 of 129, last failure 7 s after the crash
failures stopped by themselves within 10 s: true
registry lists only catalog-B at the end: [catalog-B]
```

Con số `failed calls` và `last failure` thay đổi giữa các lần chạy (khoảng 30 lời gọi lỗi, lỗi cuối cùng vào giây thứ 7 hoặc 8 sau khi sập); các dòng còn lại phải y hệt.

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
| Dòng `shop sees 2 instances` ra `false` | Máy chậm, ba tiến trình chưa kịp đăng ký trong 40 giây | Chạy lại; xem `target/logs/*.log` của từng tiến trình để tìm lỗi khởi động |
| Ở shop có log `No servers available for service: localhost` | Bean `RestClient.Builder` được gắn `@LoadBalanced` là bean duy nhất nên cả client Eureka cũng dùng nó và cố tìm sổ đăng ký qua chính cơ chế chia tải | Khai báo thêm một `RestClient.Builder` thường có `@Primary` như trong `ShopApplication`, và tiêm bản chia tải bằng `@LoadBalanced` |
| Dịch vụ báo lỗi thiếu bean `TransportClientFactories` | Có cả eureka-server (kéo theo Jersey) và eureka-client trên cùng classpath | Đặt `eureka.client.jersey.enabled=false` cho tiến trình client, như `DiscoveryDemo` làm |
