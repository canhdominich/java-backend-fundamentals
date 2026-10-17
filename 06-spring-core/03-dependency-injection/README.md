# Bài 03 - Dependency Injection và Autowiring

Mã nguồn đi kèm bài học **Dependency Injection và Autowiring** (chương 6: Spring Core) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: ba kiểu tiêm (constructor, setter, trường) bên trong container và bên ngoài nó, cách container chọn bean khi có hai ứng viên (`@Qualifier`, `@Primary`, và cái nào thắng), phụ thuộc tùy chọn (`Optional`, `ObjectProvider`, `required = false`), tiêm cả danh sách và bản đồ bean theo thứ tự `@Order`, và `@Value` với giá trị mặc định cùng cái bẫy khi thiếu giá trị.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Framework | 7.0.9 | khai báo trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
03-dependency-injection/
  pom.xml
  src/main/java/dev/canhdominich/spring/di/
    PaymentService.java, PaypalPayment.java, StripePayment.java   interface và hai cài đặt
    InjectionStylesDemo.java     ba kiểu tiêm, trong và ngoài container
    ResolutionDemo.java          hai ứng viên, @Qualifier, @Primary
    OptionalDemo.java            phụ thuộc tùy chọn khi không có bean
    CollectionDemo.java          tiêm List và Map, thứ tự @Order
    ValueDemo.java               @Value, giá trị mặc định, thiếu giá trị
  src/main/resources/
    app.properties               hai thuộc tính cho @Value
  src/test/java/dev/canhdominich/spring/di/
    DependencyInjectionTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.di.ResolutionDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.di.OptionalDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.di.CollectionDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.di.ValueDemo
```

## Kết quả mong đợi

Lệnh đầu chạy `InjectionStylesDemo`. Spring ghi cảnh báo khởi động ra stderr; dưới đây chỉ chép dòng của chương trình.

`InjectionStylesDemo`:

```text
inside the container: constructor=paypal, setter=paypal, field=paypal
outside the container, setter injection: NullPointerException on first use
outside the container, field injection: NullPointerException on first use
constructor injection outside a container: new ConstructorOrder() does not compile without a PaymentService
```

`ResolutionDemo`:

```text
two candidates, nothing to choose with: NoUniqueBeanDefinitionException: No qualifying bean of type 'dev.canhdominich.spring.di.PaymentService' available: expected single matching bean but found 2: paypal,stripe
@Qualifier("stripe") selects: stripe
@Primary on paypal, plain parameter selects: paypal
@Primary on paypal and @Qualifier("stripe") on the parameter selects: stripe
```

`OptionalDemo`:

```text
no PaymentService bean at all: Optional=none, ObjectProvider=none, required=false setter=none
```

`CollectionDemo`:

```text
List<PaymentService> in @Order: [stripe, paypal]
Map<String, PaymentService> keys: [paypal, stripe]
```

`ValueDemo`:

```text
name=Course Platform, owner=Carnell, maxSize=100
placeholder without a value and without a default: context started, field = "${app.missing}"
with PropertySourcesPlaceholderConfigurer: PlaceholderResolutionException: Could not resolve placeholder 'app.missing' in value "${app.missing}"
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
| Không thấy dòng nào của `ValueDemo` về thiếu giá trị | Chương trình in thẳng khi chạy `main` | Chạy đúng lệnh `ValueDemo` ở trên, đọc cả ba dòng |
| `mvn` không tìm thấy lớp chính | Quên `compile` ở lệnh đầu tiên sau khi sửa code | Chạy `mvn -q compile exec:exec ...` |
