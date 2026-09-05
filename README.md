# AutoTrade — mod tự động tạo order & bán AH

Mod Fabric client cho server riêng của Thành (replica DonutSMP). Tự động:
1. **SELL_AH** — cầm item, bán theo lô nhỏ (mặc định 1 stack/lần), giá = median giá thấp nhất hiện tại trên `/ah` trừ đi một chút (`sellUndercutPercent`), đọc lại giá trước mỗi lô để tránh dùng giá cũ cho cả đợt lớn.
2. **CREATE_BUY_ORDER** — quét median giá `/ah` + median giá order khác (nếu có) cho cùng item, trừ phí sàn (`ahFeePercent`), chỉ tạo order nếu lời sau phí vượt ngưỡng tối thiểu (`minProfitPercentAfterFee`). Số lượng đặt mua tự tính theo % số dư (`balanceSpendCapPercent`, đọc qua `/balance`) và % nguồn cung đang thấy trên `/ah` (`supplyCapPercent`), có trần tuyệt đối (`absoluteMaxQuantityPerAction`).

Đóng gói thành **1 file .jar duy nhất** sau khi build, bỏ vào `.minecraft/mods/` y như `mummy-auto-sell` — chỉ cần thêm sẵn file **Fabric API** cùng thư mục (mọi mod Fabric dùng tick event/command đều cần cái này, không phải do mod này làm phức tạp thêm).

## Cách dùng
- **Bật/tắt/nạp lại config**: nhấn phím tắt (mặc định `]`, đổi được trong Options → Controls → mục "AutoTrade") để mở 1 menu trong game với nút bấm — không cần gõ lệnh chat.
- **Calibrate / xem chat gần đây**: 2 việc này **bắt buộc phải là lệnh chat** (`/autotrade calibrate`, `/autotrade chatdump`) vì cần chạy trong lúc `/order` hoặc `/ah` đang mở — mở menu riêng sẽ đóng mất GUI đó nên không đưa vào nút bấm được. Đây là lệnh **client-side qua Fabric**, gõ xong KHÔNG gửi gì lên server (server không thấy), chỉ kích hoạt code trong máy bạn.

Không làm phần "deliver vào order người khác đã đặt" — đã quyết định bỏ vì margin thấp.

Đọc giá bằng **median của N slot liên tiếp** (`priceSampleSlotCount`, mặc định 5) thay vì chỉ 1 slot, để 1 listing giá rác/troll không làm lệch cả kết quả.

## ⚠️ Đọc trước khi chạy thật

Toàn bộ số **slot index** trong `autotrade.json` (chest icon, sign search, filter phễu, ô kết quả...)
là suy đoán từ ảnh chụp bạn gửi — mình không có cách chạy thử trên server thật của bạn để
xác nhận. Tương tự, cách "nhập text" (gõ vào chat sau khi bấm sign, hay server mở
`SignEditScreen` thật) cũng chỉ là giả định (`textInputMethod: "CHAT"` mặc định).

**Trước khi `/autotrade start`, luôn:**
1. Build mod, vào server, gõ `/order` hoặc `/ah`.
2. Trong khi GUI đang mở, gõ `/autotrade calibrate` — mod in ra chat toàn bộ index + tên item
   + dòng đầu tooltip của mọi slot có item trong GUI đó.
3. Đối chiếu với ảnh bạn đã chụp, sửa lại các số trong `config/autotrade.json` (nằm trong
   thư mục `.minecraft/config/` sau lần chạy đầu) cho khớp:
   - `orderCreateChestSlotIndex`, `orderSearchSignSlotIndex`, `orderFilterFunnelSlotIndex`
   - `ahSearchSignSlotIndex`, `ahFilterFunnelSlotIndex`
   - `searchSuggestionResultSlotIndex`, `firstResultItemSlotIndex`
   - `orderFilterClicksToMostPaid` (nhắm tới "Most Paid"), `ahFilterClicksToLowestPrice` (nhắm tới "Lowest Price" — 2 GUI có danh sách filter KHÁC LABEL nhau, xem README phần Filter bên dưới).
4. Nếu bấm sign xong KHÔNG thấy prompt trong chat mà là một màn hình chỉnh sign thật hiện ra
   → đổi `textInputMethod` thành `"SIGN"` và báo lại, phần gửi text trong
   `TradeStateMachine.pushTypeText()` cần đổi từ gửi chat sang gửi gói chỉnh sign
   (`UpdateSignC2SPacket`) — hiện code đang tạm gửi chat cho cả 2 trường hợp, chỗ này
   cần sửa tay nếu server dùng sign thật.
5. Gõ `/balance` xem chat trả lời dạng gì, rồi gõ `/autotrade chatdump` để xem mod có bắt
   được dòng đó không (cần có ký hiệu `$`, dùng chung parser với giá item).
6. Thử `/ah sell <giá>` liên tục tới khi đầy slot, xem server báo lỗi gì trong chat, điền
   đúng cụm từ đó vào `fullSlotErrorKeyword` (mặc định đang để tạm `"reached the maximum"`).
7. Chưa xác nhận được liệu tooltip giá là **theo đơn vị** ("$X each") hay **tổng cả stack**
   cho item dạng block/stack-64 — mặc định `assumeTooltipPriceIsPerUnit: true` (đúng với
   Totem of Undying theo ảnh đã xem). Nếu sai với item stack, đổi thành `false`.
8. Test với item rẻ, số lượng nhỏ trước, đứng cạnh quan sát — đừng chạy `/autotrade start`
   không giám sát lần đầu.

## Filter: /order vs /ah khác label

- `/order`: **Most Per Item / Most Paid / Recently Listed** — dùng "Most Paid" khi đọc giá order khác để so margin.
- `/ah`: **Lowest Price / Highest Price / Recently Listed** — dùng "Lowest Price" khi đọc giá thị trường.

Nút phễu có vẻ là nút "cycle" qua danh sách — `orderFilterClicksToMostPaid` / `ahFilterClicksToLowestPrice` là số lần click cần thiết để dừng đúng ở lựa chọn đó, xác định qua calibrate.

## Cấu trúc

- `AutoTradeMod` — entrypoint, đăng ký phím tắt mở `AutoTradeMenuScreen` (start/stop/reload), đăng ký lệnh chat `/autotrade calibrate|chatdump` (chỉ 2 lệnh này còn ở dạng chat, lý do xem mục "Cách dùng"), chạy `tick()` mỗi client tick, khởi tạo `ChatBuffer`.
- `gui/AutoTradeMenuScreen` — màn hình trong game (nút Start/Stop, Reload config, Đóng) mở bằng phím tắt.
- `TradeStateMachine` — logic chính: quét túi đồ theo `triggerAmount`, đọc median giá qua nhiều slot, tính margin/lời sau phí, tính số lượng theo số dư + cung, bán theo lô có rescan, phát hiện đầy slot AH qua chat.
- `ScriptRunner` / `ScriptStep` — hàng đợi các bước có delay giữa mỗi bước (tick-based), tránh block tick chính của game.
- `ScreenUtil` — đọc title/slot/tooltip của GUI đang mở, click slot, gửi chat/lệnh.
- `InventoryHelper` — tìm item trong túi, swap vào hotbar bằng slot mapping chuẩn của `PlayerScreenHandler` (0-8 hotbar, 9-35 túi chính khi có GUI player inventory mở) — phần này KHÔNG cần calibrate vì là chuẩn vanilla.
- `PriceParser` — parse `"$89K each"` → `89000` (hỗ trợ K/M/B), có `median()` để gộp nhiều slot.
- `ChatBuffer` — giữ 25 dòng chat gần nhất, dùng để đọc `/balance` và phát hiện lỗi đầy slot AH.
- `config/AutoTradeConfig.java`, `TradeItemConfig.java` — lưu ở `config/autotrade.json`.

## Cấu hình item cần theo dõi

Sửa `config/autotrade.json`, mảng `items`:
```json
{
  "itemSearchName": "Totem of Undying",
  "mode": "SELL_AH",
  "triggerAmount": 8
}
```
`mode`: `SELL_AH` hoặc `CREATE_BUY_ORDER`. `triggerAmount` = ngưỡng số lượng trong túi để kích hoạt (chỉ dùng cho SELL_AH — số lượng mua của CREATE_BUY_ORDER giờ tự tính theo số dư/cung, không dùng `triggerAmount`).

## Chưa làm (để sau nếu cần)

- GUI trong-game để chọn item (thay vì sửa tay JSON) — có thể tái dùng phần GUI của `mummy-auto-sell` như đã bàn.
- Deliver vào order người khác — bỏ theo quyết định margin thấp không đáng làm.
- Tối ưu "đường cong giá theo lô" (bán lô nhỏ đôi khi được giá/đơn vị cao hơn lô to) — ý hay nhưng cần đọc giá+số lượng của NHIỀU listing khác nhau để dựng đường cong, để sau khi có dữ liệu thật từ server bạn.
- Tự động chọn item "tốt nhất" trong toàn bộ danh sách item của server — không khả thi tốc độ (mỗi lần check giá tốn vài giây do phải thao tác GUI thật); hiện chỉ xoay vòng trong danh sách item bạn tự cấu hình.
