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
| 2. JDBC | CRUD và các loại Statement | [02-jdbc/03-crud-va-statement](02-jdbc/03-crud-va-statement) |
| 2. JDBC | Transaction và Batch | [02-jdbc/04-transaction-va-batch](02-jdbc/04-transaction-va-batch) |
| 2. JDBC | DAO và Repository thuần JDBC | [02-jdbc/05-dao-va-repository](02-jdbc/05-dao-va-repository) |
| 3. Servlet | Giới thiệu Servlet và cách hoạt động | [03-servlet/01-gioi-thieu-servlet](03-servlet/01-gioi-thieu-servlet) |
| 3. Servlet | Vòng đời của Servlet | [03-servlet/02-vong-doi-servlet](03-servlet/02-vong-doi-servlet) |
| 3. Servlet | Request, Response và Session | [03-servlet/03-request-response-session](03-servlet/03-request-response-session) |
| 3. Servlet | Config, Dispatcher và Filters | [03-servlet/04-config-dispatcher-filters](03-servlet/04-config-dispatcher-filters) |
| 4. JSP | Giới thiệu và kiến trúc JSP | [04-jsp/01-gioi-thieu-jsp](04-jsp/01-gioi-thieu-jsp) |
| 4. JSP | Vòng đời và thành phần JSP | [04-jsp/02-vong-doi-thanh-phan-jsp](04-jsp/02-vong-doi-thanh-phan-jsp) |
| 4. JSP | So sánh Servlet và JSP, ứng dụng MVC đăng nhập | [04-jsp/03-servlet-va-jsp-mvc](04-jsp/03-servlet-va-jsp-mvc) |
| 5. Hibernate | Giới thiệu và kiến trúc Hibernate | [05-hibernate/01-gioi-thieu-kien-truc](05-hibernate/01-gioi-thieu-kien-truc) |
| 5. Hibernate | Cấu hình và CRUD với Hibernate | [05-hibernate/02-cau-hinh-va-crud](05-hibernate/02-cau-hinh-va-crud) |
| 5. Hibernate | Annotations, Mapping và HQL | [05-hibernate/03-annotation-mapping-hql](05-hibernate/03-annotation-mapping-hql) |
| 5. Hibernate | Hiệu năng Hibernate và khóa dữ liệu | [05-hibernate/04-hieu-nang-va-lock](05-hibernate/04-hieu-nang-va-lock) |
| 5. Hibernate | Criteria, bộ nhớ đệm và kế thừa | [05-hibernate/05-criteria-cache-ke-thua](05-hibernate/05-criteria-cache-ke-thua) |
