# Câu hỏi phỏng vấn về Shop API

Mỗi câu trả lời dựa trên điều dự án này thực sự làm và đo được. Khi phỏng vấn hãy nói thêm về cái giá và cách bạn đã kiểm chứng.

## Thiết kế và dữ liệu

**1. Vì sao trừ kho bằng một câu `UPDATE` có điều kiện thay vì đọc rồi ghi?**
Đọc rồi ghi cho phép hai yêu cầu cùng đọc số cũ rồi cùng ghi. Trong test đồng thời với 20 người mua một mặt hàng còn 10, bản đọc rồi ghi nhận 20 người mua và cột tồn kho vẫn báo 9; bản `UPDATE ... WHERE stock >= :quantity` tạo đúng 10 đơn, từ chối 10 yêu cầu bằng `409` và để tồn kho bằng 0.

**2. Ràng buộc `CHECK (stock >= 0)` ở cơ sở dữ liệu có còn cần khi đã có điều kiện trong `UPDATE`?**
Có, là lưới an toàn cuối cùng: một câu lệnh khác (script bảo trì, một lỗi sau này) cố ghi số âm sẽ bị MySQL từ chối với lỗi 3819 `Check constraint 'ck_products_stock' is violated`. Lưu ý Spring dịch lỗi này thành `UncategorizedSQLException` chứ không phải `DataIntegrityViolationException`, nên nếu muốn trả lỗi riêng phải bắt đúng loại.

**3. Vì sao tiền lưu bằng số nguyên `price_cents`?**
Tránh sai số số thực khi cộng và nhân. `total_cents` là tổng của `quantity * unit_price_cents`, tính bằng `long`.

**4. Vì sao chép đơn giá vào `order_lines`?**
Giá sản phẩm đổi theo thời gian; đơn đã đặt phải giữ giá lúc đặt.

**5. Vì sao để Flyway quản lý lược đồ và đặt Hibernate ở `validate`?**
Lược đồ có lịch sử và kiểm soát phiên bản, giống mã. `validate` làm ứng dụng không khởi động nếu thực thể và bảng lệch nhau; test `theMigrationsMatchTheEntities` chạy trên MySQL thật bắt điều này sớm.

**6. Thứ tự xử lý các dòng trong một đơn có ý nghĩa gì?**
Xử lý theo `productId` tăng dần để hai đơn cùng chạm nhiều sản phẩm khóa các hàng theo cùng một thứ tự, tránh khóa chết.

## Giao dịch và đồng thời

**7. Đơn có hai dòng, dòng thứ hai thiếu hàng. Điều gì xảy ra với dòng thứ nhất?**
Cả hai nằm trong một giao dịch nên toàn bộ bị hoàn tác: test `anOrderThatFailsHalfWayRollsBackTheLinesAlreadyReserved` xác nhận tồn kho của sản phẩm thứ nhất vẫn nguyên.

**8. Vì sao `OrderService.place` không có `@Transactional` mà gọi sang `OrderPlacement`?**
Khi hai yêu cầu cùng khóa idempotency chạy song song, yêu cầu thua vi phạm ràng buộc duy nhất lúc commit. Nếu lỗi xảy ra trong cùng bean giao dịch thì không còn giao dịch để đọc lại đơn đã tạo; tách ra bean riêng cho phép bắt lỗi sau khi giao dịch đã kết thúc rồi đọc đơn thắng cuộc. Test: 6 yêu cầu cùng khóa cho 1 đơn tạo mới, 5 phản hồi lặp lại và tồn kho giảm đúng 1.

**9. Idempotency-Key hoạt động thế nào và giới hạn của nó?**
Khóa gắn với người dùng; `(user_id, idempotency_key)` là ràng buộc duy nhất. Gửi lại cùng khóa trả đơn cũ với `Idempotent-Replayed: true`. Giới hạn: nếu gửi lại cùng khóa với nội dung khác, dự án vẫn trả đơn cũ mà không so sánh nội dung; một thiết kế chặt hơn sẽ lưu băm của yêu cầu và trả lỗi khi khác.

**10. Khóa lạc quan (`@Version`) dùng ở đâu?**
Ở thực thể `Product`. Câu `UPDATE` trừ kho tự tăng `version`, nên một quản trị viên đọc sản phẩm trước rồi lưu sau khi có đơn vừa trừ kho sẽ bị từ chối (`ObjectOptimisticLockingFailureException`, trả `409` với mã `CONCURRENT_UPDATE`) thay vì ghi đè tồn kho mới bằng số cũ.

## Bảo mật

**11. Vì sao dùng JWT không lưu phiên và đánh đổi gì?**
Không cần kho phiên chung nên dễ mở rộng ngang; đổi lại token không thu hồi được trước khi hết hạn (dự án nêu rõ hạn chế này). Có thể giảm thời hạn và thêm refresh token.

**12. Token bị sửa hoặc hết hạn thì sao?**
Hết hạn không chính xác tại thời điểm `exp`: Spring chấp nhận lệch đồng hồ 60 giây; đo với `JWT_TTL=PT2S`, token vẫn dùng được sau 30 giây và bị từ chối sau 62 giây. Token bị sửa hoặc hết hạn rõ ràng nhận `401` dạng `application/problem+json` kèm `WWW-Authenticate`; test dùng một token hợp lệ về chữ ký nhưng đã hết hạn và một token bị đổi vài ký tự cuối.

**13. Vì sao đọc đơn của người khác trả `404` chứ không phải `403`?**
Để không tiết lộ đơn đó tồn tại. Quản trị viên đọc được mọi đơn.

**14. Vì sao email không tồn tại và mật khẩu sai trả đúng cùng một phản hồi?**
Để không cho kẻ tấn công dò tài khoản nào tồn tại; test so sánh hai thân phản hồi và chúng giống hệt. Nhưng thân giống nhau chưa đủ: lúc đầu đăng nhập với email không tồn tại mất khoảng 23 ms còn với email có thật mất khoảng 111 ms (chi phí băm BCrypt), tức thời gian vẫn lộ tài khoản có tồn tại. Cách sửa: luôn so mật khẩu với một băm giả khi không tìm thấy người dùng; sau sửa cả ba trường hợp (đúng, sai mật khẩu, không tồn tại) mất khoảng 119 đến 124 ms.

**15. Điều gì xảy ra nếu `JWT_SECRET` ngắn?**
Ứng dụng không khởi động (`app.jwt.secret ... must be at least 32 bytes long`). Thà dừng lúc khởi động còn hơn chạy với khóa yếu.

## API và vận hành

**16. Vì sao có lớp `PageResponse` riêng thay vì trả `Page` của Spring?**
`Page` mang cấu trúc nội bộ (`pageable`, `sort`...) dễ đổi giữa các phiên bản; hợp đồng API nên do dự án quy định: `content`, `page`, `size`, `totalElements`, `totalPages`.

**17. Danh sách đơn không có các dòng chi tiết; vì sao?**
Tránh truy vấn N+1 và phản hồi lớn. Chi tiết đơn dùng `@EntityGraph` nạp dòng, sản phẩm và người dùng bằng một truy vấn.

**18. Bạn kiểm thử đồng thời thế nào?**
Một nhóm luồng cùng chờ một `CountDownLatch` rồi cùng gửi yêu cầu qua `MockMvc` vào MySQL thật; kết quả được khẳng định trên cả mã trạng thái, tồn kho và tổng số lượng đã bán trong `order_lines`.

**19. Vì sao test chạy trên MySQL thật bằng Testcontainers thay vì H2?**
Hành vi khác nhau (ràng buộc `CHECK`, khóa hàng, so khớp chữ hoa chữ thường). Test đồng thời và test ràng buộc chỉ có ý nghĩa trên cơ sở dữ liệu giống sản xuất. Cái giá: cần Docker và bộ test mất khoảng 195 giây.

**20. Bạn đóng gói và vận hành thế nào?**
Image nhiều tầng (371 MB, chạy bằng người dùng không phải root), Compose với healthcheck, giới hạn bộ nhớ 512 MiB, `server.shutdown=graceful`, bí mật qua biến môi trường. Khởi động lại dịch vụ API mất khoảng 35 giây tới khi sẵn sàng; sao lưu bằng `mysqldump` và đã tập khôi phục vào một cơ sở dữ liệu thử.

## Câu hỏi mở để bạn chuẩn bị

* Nếu lưu lượng tăng gấp 100 lần, điểm nghẽn đầu tiên là gì và bạn đo nó thế nào?
* Bạn thêm thanh toán vào đây ra sao nếu nó gọi một dịch vụ bên ngoài? (gợi ý: saga, idempotency, hết thời gian chờ, bù trừ như chương Microservices)
* Làm sao để triển khai không gián đoạn?
* Bạn sẽ bảo vệ `/api/auth/login` khỏi dò mật khẩu bằng cách nào?
