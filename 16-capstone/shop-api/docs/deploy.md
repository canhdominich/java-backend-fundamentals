# Hướng dẫn triển khai

Hướng dẫn này dùng Docker Compose trên một máy Linux. Các lệnh và số đo bên dưới đã được chạy với Docker Engine 23.0.2 và Compose 2.17.2.

## 1. Chuẩn bị máy

* Cài Docker Engine và Docker Compose plugin.
* Mở cổng cho reverse proxy (80 và 443) và **không** mở cổng MySQL ra ngoài. Trong `docker-compose.yml` MySQL không có `ports`, nên chỉ dịch vụ `api` trong mạng của Compose truy cập được.
* Ít nhất 1 GB bộ nhớ trống: đo được `api` dùng khoảng 329 MiB (giới hạn 512 MiB) và `mysql` khoảng 445 MiB.

## 2. Cấu hình bí mật

```bash
cp .env.example .env
```

Đặt các giá trị thật vào `.env` và giữ tệp này ngoài Git (`.gitignore` đã loại nó):

| Biến | Yêu cầu |
|---|---|
| `JWT_SECRET` | Chuỗi ngẫu nhiên từ 32 byte trở lên, ví dụ `openssl rand -base64 48`; ứng dụng không khởi động nếu ngắn hơn |
| `DB_PASSWORD`, `DB_ROOT_PASSWORD` | Mật khẩu mạnh, khác nhau |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Tài khoản quản trị đầu tiên; đổi mật khẩu ngay sau lần đăng nhập đầu |
| `SPRING_PROFILES_ACTIVE` | Để trống ở sản xuất: profile `dev` nạp sản phẩm mẫu |

## 3. Build và chạy

```bash
mvn -DskipTests package
docker compose up -d --build --wait
```

Lần chạy đầu trên máy đo được mất khoảng 89 giây (gồm kéo image, khởi tạo MySQL và dựng image ứng dụng). `--wait` chờ cả hai dịch vụ báo `healthy`. Kiểm tra:

```bash
docker compose ps
curl -s localhost:8080/actuator/health
ADMIN_PASSWORD=... bash scripts/smoke.sh
```

## 4. HTTPS và reverse proxy

Ứng dụng nói HTTP thuần ở cổng 8080. Đặt một reverse proxy (Nginx, Caddy hoặc bộ cân bằng tải của nhà cung cấp) đứng trước để kết thúc TLS và chuyển yêu cầu tới `localhost:8080`; chỉ publish cổng 8080 vào địa chỉ nội bộ (`127.0.0.1:8080:8080` trong `ports`). Phần này chưa được kiểm chứng trong dự án.

## 5. Nâng cấp phiên bản

```bash
git pull
mvn -DskipTests package
APP_VERSION=1.0.1 docker compose up -d --build api
```

Flyway chạy các migration mới lúc khởi động. Đo được khởi động lại dịch vụ `api` (tới khi `/actuator/health` báo `UP`) mất khoảng 35 giây trong lúc đó yêu cầu mới nhận lỗi kết nối. Dịch vụ dùng `server.shutdown=graceful`, nên yêu cầu đang chạy được hoàn tất khi nhận tín hiệu dừng. Muốn không gián đoạn cần chạy nhiều bản sau một bộ cân bằng tải, điều dự án này chưa làm.

**Quy tắc migration:** không sửa tệp `V1__schema.sql` đã chạy ở môi trường nào; thay đổi lược đồ luôn thêm tệp `V{n}__mo_ta.sql` mới. Flyway so mã băm và báo lỗi nếu tệp cũ bị sửa.

## 6. Sao lưu và khôi phục

```bash
docker compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --no-tablespaces --single-transaction shop' > backup.sql
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "create database shop_restore"'
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" shop_restore' < backup.sql
```

Đo được với dữ liệu mẫu: sao lưu mất khoảng 1 giây, tệp 7.681 byte, và khôi phục cho đúng số dòng ở cả bốn bảng (3 người dùng, 5 sản phẩm, 2 đơn, 2 dòng đơn). Hãy tập khôi phục vào một cơ sở dữ liệu thử như trên ít nhất một lần trước khi cần đến nó; một bản sao lưu chưa từng được khôi phục chưa phải là bản sao lưu. Lưu tệp sao lưu ngoài máy chủ và đặt lịch chạy lệnh đầu bằng cron.

## 7. Gỡ lỗi

| Triệu chứng | Kiểm tra |
|---|---|
| `api` không bao giờ `healthy` | `docker compose logs api`: thiếu `JWT_SECRET` hoặc ngắn hơn 32 byte, không nối được MySQL, migration lỗi |
| `docker compose up` báo thiếu biến | Compose dùng `${VAR:?...}` nên báo ngay biến còn thiếu trong `.env` |
| Mọi yêu cầu `401` | Token hết hạn (mặc định 1 giờ) hoặc `JWT_SECRET` đã đổi giữa các lần chạy |
| `409 OUT_OF_STOCK` | Tồn kho không đủ; kiểm tra `GET /api/products/{id}` |

## 8. Danh sách kiểm tra trước khi mở ra Internet

* [ ] Đổi mọi mật khẩu mặc định trong `.env`; `.env` không nằm trong Git.
* [ ] Đặt reverse proxy với TLS; không công khai cổng MySQL.
* [ ] Có lịch sao lưu và đã thử khôi phục.
* [ ] Giới hạn tốc độ gọi `/api/auth/login` ở reverse proxy (dự án chưa có).
* [ ] Theo dõi `/actuator/health` và log của container.
