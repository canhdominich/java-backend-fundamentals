# Bài 03 - Đóng gói và chạy microservices bằng Docker Compose

Mã nguồn đi kèm bài học **Đóng gói và chạy microservices bằng Docker Compose** (chương 14: Microservices) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: hai dịch vụ `inventory-service` và `order-service` đóng thành hai image Docker, chạy cùng nhau bằng Docker Compose. Bạn đo kích thước image và so Dockerfile nhiều tầng (layered jar) với Dockerfile chép nguyên tệp jar khi sửa một dòng mã; xem dịch vụ gọi nhau bằng tên trong mạng của Compose (`http://inventory:8081`) và chờ nhau bằng healthcheck; đo bộ nhớ heap của JVM trong container có giới hạn 256 MB với và không có `-XX:MaxRAMPercentage`; và đo điều xảy ra khi dừng một dịch vụ đang xử lý một yêu cầu 5 giây, với tắt êm (`server.shutdown=graceful`) và tắt ngay.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml` gốc |
| Cổng trên máy | 18080, 18081, 18082 | cổng đã ánh xạ ra máy; đổi trong `docker-compose.yml` nếu bị chiếm |
| curl, bash, python3 | bản gần đây | `demo.sh` dùng để gọi dịch vụ và tính thời gian |
| Docker | bất kỳ bản gần đây | Docker Engine 23 và Docker Compose 2.17 đã kiểm chứng; cần kéo image `eclipse-temurin:17-jre` |

## Cấu trúc

```text
03-docker-compose/
  pom.xml                       aggregator: hai module, parent Boot 4.1.1
  docker-compose.yml            hai dịch vụ, mạng, healthcheck, giới hạn bộ nhớ, profile compare
  demo.sh                       build, khởi động, đo và in kết quả
  inventory-service/
    pom.xml, Dockerfile, Dockerfile.simple, .dockerignore
    src/main/resources/application.properties
    src/main/java/dev/canhdominich/micro/inventory/
      InventoryApplication.java, InventoryController.java   /reserve, /stock/{sku}, /slow, /memory
  order-service/
    pom.xml, Dockerfile, Dockerfile.simple, .dockerignore
    src/main/resources/application.properties
    src/main/java/dev/canhdominich/micro/order/
      OrderApplication.java, OrderController.java           /orders gọi inventory theo INVENTORY_URL
```

## Chạy thí nghiệm

```bash
bash demo.sh
```

Lần đầu Docker kéo image `eclipse-temurin:17-jre` (khoảng 270 MB) và script mất vài phút; các lần sau nhanh hơn. Script sửa tạm một dòng của `InventoryController` để đo việc build lại, rồi khôi phục tệp.

## Kết quả mong đợi

Kết quả của `demo.sh`:

```text
-- image
layered image: 327MB
layer with the libraries: 23.6MB
layer with your classes: 6.39kB
single-layer image copies the whole jar in one layer: 23.8MB
-- after changing one line of code
layered build reused the libraries layer: yes
single-layer build reused the jar layer: no
-- start the system
healthy after about 16 s
order service reaches inventory at: http://inventory:8081
order through the compose network: status 200, stock now 98
inventory is also reachable from the host: 200
-- memory limit 256 MB
with -XX:MaxRAMPercentage=75: max heap 185 MB, processors 4
with default JVM options: max heap 121 MB, processors 4
-- stopping a service that is busy with a 5 s request
shutdown graceful: stop took about 4 s, the request got status 200, container exit code 143
shutdown immediate: stop took about 1 s, the request got status 000, container exit code 143
```

Con số về kích thước, thời gian và heap phụ thuộc máy và phiên bản Docker; các dòng `yes` và `no`, trạng thái 200 và 000, mã thoát 143 và địa chỉ `http://inventory:8081` phải y hệt.

## Dọn dẹp

```bash
mvn clean
```

## Lỗi thường gặp khi chạy lab

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Dịch vụ `unhealthy` dù ứng dụng đã chạy | Healthcheck viết bằng `CMD-SHELL` dùng `/bin/sh`, không hỗ trợ `/dev/tcp` | Dùng `["CMD", "bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8081"]` như tệp mẫu |
| `port is already allocated` | Cổng 18080 đến 18082 đã bị chiếm | Đổi cổng bên trái trong `docker-compose.yml` và biến `INVENTORY`, `ORDER`, `SIMPLE` trong `demo.sh` |
| `docker compose down` báo network còn endpoint | Container của profile `compare` vẫn chạy | Dùng `docker compose --profile compare down` |
