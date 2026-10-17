# Bài 04 - Vòng đời, scope và SpEL

Mã nguồn đi kèm bài học **Bean lifecycle, scope và SpEL** (chương 6: Spring Core) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: mười một callback của một bean theo đúng thứ tự (constructor, tiêm, `BeanNameAware`, `BeanPostProcessor`, `@PostConstruct`, `afterPropertiesSet`, `initMethod`, rồi các callback hủy), khác biệt singleton và prototype (kể cả việc container không gọi callback hủy của prototype), cái bẫy tiêm prototype vào singleton cùng hai cách sửa (`ObjectProvider`, scoped proxy), và SpEL: biểu thức độc lập và `@Value` trộn `${...}` với `#{...}`.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Framework | 7.0.9 | khai báo trong `pom.xml` |
| jakarta.annotation-api | 3.0.0 | cung cấp `@PostConstruct` và `@PreDestroy` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
04-vong-doi-scope-spel/
  pom.xml
  src/main/java/dev/canhdominich/spring/lifecycle/
    Events.java                  ghi lại và in sự kiện kèm số thứ tự
    Engine.java                  phụ thuộc đơn giản được tiêm vào Probe
    Probe.java                   bean có đủ các loại callback
    Tracer.java                  BeanPostProcessor ghi hai mốc trước và sau khởi tạo
    LifecycleConfig.java         khai báo bean, initMethod và destroyMethod
    LifecycleDemo.java           chạy vòng đời và in thứ tự
    ScopeDemo.java               singleton và prototype, callback hủy
    TrapDemo.java                prototype trong singleton và hai cách sửa
    Student.java                 đối tượng mẫu cho SpEL
    SpelDemo.java                biểu thức SpEL và @Value
  src/main/resources/
    report.properties            giá trị report.hours cho @Value
  src/test/java/dev/canhdominich/spring/lifecycle/
    LifecycleScopeSpelTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.lifecycle.ScopeDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.lifecycle.TrapDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.lifecycle.SpelDemo
```

## Kết quả mong đợi

Lệnh đầu chạy `LifecycleDemo`. Spring ghi cảnh báo khởi động ra stderr; dưới đây chỉ chép dòng của chương trình.

`LifecycleDemo`:

```text
-- creating the context
  1. constructor
  2. dependency injected
  3. BeanNameAware.setBeanName(probe)
  4. BeanPostProcessor before initialization
  5. @PostConstruct
  6. InitializingBean.afterPropertiesSet
  7. initMethod
  8. BeanPostProcessor after initialization
-- context is up, bean is ready to use
-- closing the context
  9. @PreDestroy
  10. DisposableBean.destroy
  11. destroyMethod
```

`ScopeDemo`:

```text
singleton ids: 1, 1
prototype ids: 1, 2
-- closing the context
  1. destroying singleton #1
destroy callbacks that ran: [destroying singleton #1]
```

`TrapDemo`:

```text
prototype injected directly into a singleton, three calls: 1, 1, 1
ObjectProvider.getObject() per call, three calls: 2, 3, 4
scoped proxy (proxyMode = TARGET_CLASS), three calls: 1, 2, 3
```

`SpelDemo`:

```text
2 * 60 * 60  ->  7200
'Hello'.length()  ->  5
T(java.lang.Math).max(3, 7)  ->  7
name  ->  Binh
score >= 5 ? 'pass' : 'fail'  ->  fail
nickname ?: 'none'  ->  none
address?.city  ->  null
address.city  ->  SpelEvaluationException EL1007E: Property or field 'city' cannot be found on null
students.?[level == 'ADVANCED'].![name]  ->  [An]
students.![score]  ->  [8, 4]
hours=2, seconds=7200, cacheSeconds=7200, shout=HI, safe=null, biggest=7
```

Số thứ tự id của đối tượng (ví dụ `1, 2`) đếm từ đầu mỗi lần chạy một chương trình riêng.

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
| `@PostConstruct` không chạy | Thiếu `jakarta.annotation-api` trên classpath | Giữ khai báo trong `pom.xml` |
| Số id của prototype khác bản README khi chạy nhiều demo cùng lúc | Bộ đếm là biến tĩnh của lớp | Chạy từng demo riêng bằng lệnh trong README |
