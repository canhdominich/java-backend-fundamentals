# Bài 03 - API Gateway với Spring Cloud Gateway

Mã nguồn đi kèm bài học **Spring Cloud Gateway** (chương 13: Spring Cloud) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: một gateway đứng trước hai dịch vụ `catalog` và `order`, với các route khai báo trong `application.yml` (khớp theo đường dẫn, bỏ tiền tố bằng `StripPrefix`, viết lại đường dẫn bằng `RewritePath`, thêm header yêu cầu và phản hồi), một route `lb://` chia lời gọi cho hai bản của catalog, một bộ lọc Servlet kiểm tra khóa API ở rìa (yêu cầu bị từ chối không tới backend), và điều xảy ra khi một backend ngừng chạy. Mọi dịch vụ chạy trong cùng một tiến trình trên cổng ngẫu nhiên.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.0.8 | khai báo ở `parent` trong `pom.xml`; Spring Cloud 2025.1.3, Gateway 5.0.3 |
| Cổng mạng | cổng ngẫu nhiên | mọi tiến trình trong lab chạy trên cổng ngẫu nhiên |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
03-api-gateway/
  pom.xml                       parent Boot 4.0.8, BOM Spring Cloud 2025.1.3, gateway server webmvc, LoadBalancer
  src/main/resources/
    application.yml             các route của gateway
  src/main/java/dev/canhdominich/cloud/
    gateway/GatewayApplication.java   ứng dụng gateway
    gateway/ApiKeyFilter.java         bộ lọc Servlet kiểm tra X-Api-Key cho đường dẫn /api/
    catalog/CatalogApplication.java   backend catalog
    catalog/CatalogController.java    /products, /echo, /admin/hits
    order/OrderApplication.java       backend order
    order/OrderController.java        /orders/{id}
    GatewayDemo.java                  chạy mọi thí nghiệm
  src/test/java/dev/canhdominich/cloud/
    GatewayBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `GatewayDemo` (đã bỏ log ở stderr):

```text
-- routing by path and removing a prefix
GET /api/catalog/products: 200 products from catalog-1
what the backend received: /echo | X-Gateway=shop-edge
response header added by the gateway: X-Served-By=gateway
-- rewriting a path
GET /api/orders/5: 200 order 5
-- a path with no route
GET /nothing: status 404
-- a route that balances over two instances
6 calls to /lb/catalog/products: {products from catalog-1=3, products from catalog-2=3}
-- a filter at the edge
GET /api/catalog/products without key: status 401
GET /api/catalog/products with wrong key: status 401
backend hits changed by the rejected calls: 0
GET /api/catalog/products with key: status 200
calling a backend directly, bypassing the gateway: status 200
-- a backend that is down
GET /api/orders/5 after stopping order: status 500
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
| `Invalid URI syntax: Illegal character in path at index 1: ${catalog.url}` khi một backend khởi động | Backend đọc cùng `application.yml` của gateway | Khởi động backend với `spring.config.name=backend` như `GatewayDemo` làm |
| Mọi đường dẫn trả 404 | Không route nào khớp đường dẫn (thí nghiệm `/nothing` cho đúng kết quả này) | Kiểm tra `Path=` của từng route và tiền tố thuộc tính `spring.cloud.gateway.server.webmvc.routes` |
