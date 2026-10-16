# Java Backend Fundamentals

Mã nguồn đi kèm khóa học **Java Backend: JDBC, Servlet, Hibernate và Spring**. Mỗi bài học có thực hành code đều có một thư mục riêng ở đây, chạy được độc lập, kèm README ghi môi trường, cách chạy và kết quả mong đợi. Số liệu và thông báo lỗi trong bài học được đo từ chính các thư mục này.

## Cách dùng

1. Đọc bài học trên nền tảng; bài có liên kết thẳng tới thư mục mã nguồn tương ứng.
2. Clone repo một lần:

   ```bash
   git clone https://github.com/canhdominich/java-backend-fundamentals.git
   ```

3. Vào thư mục của bài, đọc `README.md` trong đó rồi chạy theo hướng dẫn.

## Quy ước thư mục

```text
<số chương>-<chủ đề>/<số bài>-<tên bài>/
```

Số chương và số bài khớp thứ tự trong khóa học. Mỗi thư mục bài là một dự án Maven độc lập có `README.md`, `pom.xml` và, khi cần dịch vụ bên ngoài, `docker-compose.yml`.

## Yêu cầu chung

| Thành phần | Phiên bản |
|---|---|
| JDK | 17 trở lên |
| Maven | 3.9 trở lên |
| Docker và Docker Compose | Cần cho các bài dùng cơ sở dữ liệu và cho bộ test dùng Testcontainers |

## Các bài đã có

| Chương | Bài | Thư mục |
|---|---|---|
| 2. JDBC | Giới thiệu JDBC và JDBC Driver | [02-jdbc/01-gioi-thieu-jdbc](02-jdbc/01-gioi-thieu-jdbc) |
| 2. JDBC | Kết nối MySQL bằng JDBC | [02-jdbc/02-ket-noi-mysql](02-jdbc/02-ket-noi-mysql) |
