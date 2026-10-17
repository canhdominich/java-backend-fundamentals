# Bài 02 - Container và cách cấu hình bean

Mã nguồn đi kèm bài học **IoC Container: BeanFactory và ApplicationContext** (chương 6: Spring Core) của khóa Java Backend: JDBC, Servlet, Hibernate và Spring.

Bạn sẽ tự chạy: `BeanFactory` trần so với `ApplicationContext` (khi nào bean được tạo, và vì sao `BeanFactory` trần không tự tiêm constructor), ba cách khai báo cùng một bean (quét component, phương thức `@Bean`, XML), các annotation stereotype và việc chúng đều là `@Component`, và ba lỗi cấu hình (thiếu bean, vòng phụ thuộc qua constructor, vòng phụ thuộc qua trường) cùng thời điểm chúng lộ ra.

## Yêu cầu môi trường

| Thành phần | Phiên bản đã kiểm chứng | Ghi chú |
|---|---|---|
| JDK | 17 trở lên | `java -version` |
| Maven | 3.9 trở lên | `mvn -v` |
| Spring Framework | 7.0.9 | khai báo trong `pom.xml` |
| Docker | không cần | bài này không dùng cơ sở dữ liệu |

## Cấu trúc

```text
02-container-va-cau-hinh/
  pom.xml
  src/main/java/dev/canhdominich/spring/container/
    Events.java                  ghi lại và in các sự kiện tạo bean
    Engine.java, Car.java        hai lớp thường: Car nhận Engine qua constructor
    EagerConfig.java             khai báo Engine và Car bằng @Bean
    LazyDemo.java                BeanFactory so với ApplicationContext
    Greeter.java                 bean dùng cho ba cách cấu hình
    scanned/                     ScannedGreeterConfig và ScannedGreeter (quét component)
    JavaGreeterConfig.java       cùng bean bằng @Bean
    ConfigStylesDemo.java        dựng ba context và so sánh
    stereo/                      bốn lớp stereotype và cấu hình quét
    StereotypeDemo.java          chứng minh stereotype là @Component
    FailuresDemo.java            ba lỗi cấu hình
  src/main/resources/
    greeter.xml                  cùng bean khai báo bằng XML
  src/test/java/dev/canhdominich/spring/container/
    ContainerBehaviourTest.java
```

## Chạy thí nghiệm

```bash
mvn -q compile exec:exec
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.container.ConfigStylesDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.container.StereotypeDemo
mvn -q exec:exec -Dmain.class=dev.canhdominich.spring.container.FailuresDemo
```

## Kết quả mong đợi

Lệnh đầu chạy `LazyDemo`. Spring ghi cảnh báo khởi động ra stderr; dưới đây chỉ chép dòng của chương trình.

`LazyDemo`:

```text
-- BeanFactory with plain definitions and no autowiring mode
getBean(car) failed: Error creating bean with name 'car': Failed to instantiate [dev.canhdominich.spring.container.Car]: No default constructor found
-- BeanFactory: register two definitions
definitions registered, beans created so far: 0
  event: constructing Engine
  event: constructing Car
after getBean(car): [constructing Engine, constructing Car]
-- ApplicationContext: build the context, call nothing else
  event: constructing Engine
  event: constructing Car
right after the constructor returned: [constructing Engine, constructing Car]
getBean(car) later changes nothing: true
```

`ConfigStylesDemo`:

```text
component scan: class=ScannedGreeter, scope=singleton, greet="hello from a scanned component"
@Bean method: class=Greeter, scope=singleton, greet="hello from a @Bean method"
XML: class=Greeter, scope=singleton, greet="hello from an XML bean"
```

`StereotypeDemo`:

```text
@Service is meta-annotated with @Component: true
@Repository is meta-annotated with @Component: true
@Controller is meta-annotated with @Component: true
beans found by scanning: [courseController, courseRepository, courseService, plainComponent]
beans carrying @Service: [courseService]
beans carrying @Repository: [courseRepository]
```

`FailuresDemo`:

```text
-- a constructor needs an Engine that nobody declared
failed at startup: NoSuchBeanDefinitionException: No qualifying bean of type 'dev.canhdominich.spring.container.Engine' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}
-- two beans need each other in their constructors
failed at startup: BeanCurrentlyInCreationException: Error creating bean with name 'first': Requested bean is currently in creation: Is there an unresolvable circular reference or an asynchronous initialization dependency?
-- two beans reference each other through @Autowired fields
started, 8 definitions
```

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
| `FailuresDemo` in dòng `Exception in thread` ở stderr | Spring tự ghi lỗi khởi động trước khi chương trình bắt nó | Bình thường; chỉ đọc dòng `failed at startup:` của chương trình |
| `ClassPathXmlApplicationContext` báo không thấy `greeter.xml` | Chạy sai thư mục hoặc chưa biên dịch | Chạy từ thư mục của bài bằng `mvn -q compile exec:exec ...` |
