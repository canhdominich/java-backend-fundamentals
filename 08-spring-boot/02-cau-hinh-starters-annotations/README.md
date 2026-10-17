# Bài 02 - Cấu hình, starter và annotation

Mã nguồn đi kèm bài học **Cấu hình, Starters và Annotations** (chương 8: Spring Boot) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: `@ConfigurationProperties` gắn `application.properties` vào một `record` (ép kiểu Duration và danh sách, tên thoáng như `max-size` và `maxSize`), thứ tự ưu tiên giữa file cơ sở, file theo profile, biến môi trường, thuộc tính hệ thống và dòng lệnh, profile bật hoặc tắt bean, cấu hình sai làm ứng dụng không khởi động (kèm `@Value` thiếu khóa), một auto-configuration tự viết với `@ConditionalOnProperty` và `@ConditionalOnMissingBean`, và giới hạn của quét component.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Boot | 4.1.1 | khai báo ở `parent` trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-cau-hinh-starters-annotations/
  pom.xml                       parent Boot 4.1.1, starter webmvc, validation, test
  src/main/java/dev/canhdominich/boot/
    config/ConfigApplication.java    @SpringBootApplication và @ConfigurationPropertiesScan
    config/AppProperties.java        record gắn với tiền tố app, có kiểm tra dữ liệu
    config/DevOnly.java              bean chỉ có ở profile dev
    config/MissingValue.java         @Value với khóa không tồn tại
    config/PrintName.java            tiến trình con đọc tên để thử biến môi trường
    config/ConfigDemo.java           chạy mọi thí nghiệm và in kết quả
    starter/GreetingProperties.java, GreetingService.java, GreetingAutoConfiguration.java   auto-configuration tự viết
    other/OverrideConfig.java        bean GreetingService do người dùng tự khai báo
    outside/OutsideComponent.java    component nằm ngoài package được quét
  src/main/resources/
    application.properties, application-dev.properties, application-prod.properties
    META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  src/test/java/dev/canhdominich/boot/config/
    ConfigBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
```

## Kết quả mong đợi

Kết quả của `ConfigDemo` (đã bỏ log ở stderr):

```text
-- binding: application.properties into one typed object
AppProperties[name=Course Platform, maxSize=10, timeout=PT30S, admins=[an, binh], mail=Mail[host=localhost, port=25]]
-- relaxed names on the command line
--app.max-size=9 -> maxSize 9
--app.maxSize=7 -> maxSize 7
--app.max_size=8 -> maxSize 8
-- precedence: which source wins for app.name
application.properties only -> Course Platform
profile dev -> Course Platform (dev)
profile dev + system property -Dapp.name=sys -> sys
profile dev + environment APP_NAME=env -> env
profile dev + environment APP_NAME=env + --app.name=cli -> cli
-- profiles: which beans exist
no profile: DevOnly present = false
profile dev: DevOnly present = true
profile dev: maxSize = 50
-- failing fast on bad configuration
--app.max-size=0 -> ConfigurationPropertiesBindException / BindValidationException: Binding validation errors on app
--app.timeout=abc -> ConfigurationPropertiesBindException / IllegalArgumentException: 'abc' is not a valid duration
@Value("${app.missing}") -> BeanCreationException / PlaceholderResolutionException: Could not resolve placeholder 'app.missing' in value "${app.missing}"
-- a small auto-configuration of our own
default -> hello, An
--greeting.message=hi -> hi, An
--greeting.enabled=false -> GreetingService beans: 0
own GreetingService bean in OverrideConfig -> custom, An
condition report for GreetingAutoConfiguration: matched
-- what component scanning covers
OutsideComponent beans (package dev.canhdominich.boot.outside) = 0
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
| Dòng "precedence ... environment" báo không tìm thấy lớp `PrintName` | Chạy bằng `exec:java` nên classpath của tiến trình con sai | Dùng `mvn -q compile exec:exec` như trong README |
| Dòng `OverrideConfig` luôn in `custom` | Đặt `OverrideConfig` trong package được quét | Giữ nó ở package `other` ngoài package của `ConfigApplication` |
