package com.thanh.autotrade.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * TOÀN BỘ số slot / số tick dưới đây là GIÁ TRỊ MẶC ĐỊNH PHỎNG ĐOÁN — mình không có
 * cách chạy thử trên server thật của bạn nên KHÔNG thể đảm bảo đúng ngay từ đầu.
 * Dùng lệnh `/autotrade calibrate` (in ScreenCalibrateCommand) để xem index thật của
 * từng slot khi bạn tự mở /order và /ah, rồi sửa lại file config này cho khớp.
 */
public class AutoTradeConfig {
    public static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("autotrade.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---- Danh sách item cần auto xử lý ----
    public List<TradeItemConfig> items = new ArrayList<>();

    // ---- Margin khi BÁN THẲNG LÊN /ah (không phải tạo order) ----
    /** List thấp hơn giá thấp nhất hiện tại bao nhiêu % để bán được nhanh. 0 = list ngang giá thấp nhất. */
    public double sellUndercutPercent = 2.0;

    // ---- Margin khi TẠO ORDER MUA (mua rẻ hơn AH để ăn chênh lệch) ----
    /** Order sẽ không rẻ hơn giá AH thấp nhất quá mức này (%) — quá rẻ thì không ai giao hàng. */
    public double marginMaxPercent = 15.0;
    /** Order phải rẻ hơn giá AH thấp nhất ít nhất mức này (%) — để đảm bảo có lời khi flip lại. */
    public double marginMinPercent = 5.0;

    // ---- Cách nhập text (search item / amount / giá) ----
    /** "CHAT" nếu server bắt gõ vào chat sau khi mở ô nhập; "SIGN" nếu nó mở màn hình sửa sign thật. */
    public String textInputMethod = "CHAT";

    /** Chưa xác nhận được: giả định tooltip giá luôn ghi theo ĐƠN VỊ (vd "$89K each"), không phải tổng cả stack.
     *  Đã thấy đúng vậy với Totem of Undying; CHƯA có ảnh xác nhận với item dạng stack 64 (block...).
     *  Nếu server bạn hiển thị giá TỔNG cho item stack, cần sửa PriceParser để chia cho ItemStack.getCount(). */
    public boolean assumeTooltipPriceIsPerUnit = true;

    /** Sau khi xử lý xong 1 vòng hết item trong danh sách (hoặc không còn gì để làm), chờ bao nhiêu tick rồi quét lại từ đầu. 3600 tick = 3 phút. */
    public int cycleCooldownTicks = 3600;

    // ---- Timing (đơn vị: tick, 20 tick = 1 giây) — tăng lên nếu mạng/server bạn lag ----
    public int delayAfterOpenGui = 10;
    public int delayAfterClickSlot = 6;
    public int delayAfterTypeText = 15;
    public int delayAfterFilterClick = 6;

    // ---- Phí & ngưỡng lời tối thiểu ----
    /** Phí sàn AH khi bán (server ghi 2%, 1% nếu có rank Donut+). */
    public double ahFeePercent = 2.0;
    /** Nếu lời sau phí (so giá order với giá AH) dưới mức này (%) thì bỏ qua, không tạo order. */
    public double minProfitPercentAfterFee = 8.0;

    // ---- Đọc giá theo median nhiều slot (chống outlier/troll list giá rác) ----
    /** Số slot liên tiếp từ firstResultItemSlotIndex sẽ được đọc giá để lấy median. */
    public int priceSampleSlotCount = 5;

    // ---- Sizing (số lượng/số tiền mỗi lần mua-tạo-order) ----
    /** Tối đa % số dư hiện có được chi trong 1 lần tạo order. */
    public double balanceSpendCapPercent = 8.0;
    /** Có giới hạn số lượng mua theo tổng cung đang thấy trên /ah không. */
    public boolean applySupplyCap = true;
    /** Nếu applySupplyCap=true: tối đa % tổng cung (tổng count các slot đã sample) được mua trong 1 lần. */
    public double supplyCapPercent = 20.0;
    /** Trần tuyệt đối số lượng mỗi lần mua/tạo order, bất kể 2 cái trên tính ra bao nhiêu. */
    public long absoluteMaxQuantityPerAction = 64;
    /** Số lô (mỗi lô = 1 lần cầm-item-rồi-/ah-sell) tối đa được bán trong 1 chu kỳ xử lý item, phần dư để lại chu kỳ sau. */
    public int maxSellBatchesPerCycle = 1;
    /** Đoạn chữ (không phân biệt hoa/thường) xuất hiện trong chat khi server báo đầy slot AH.
     *  CHƯA XÁC NHẬN — bạn cần tự thử /ah sell khi đã đầy slot rồi xem server báo gì, điền lại cho đúng. */
    public String fullSlotErrorKeyword = "reached the maximum";

    // ---- Slot index trong GUI /order (theo ảnh "Orders (Page 1)") — CẦN CALIBRATE LẠI ----
    public int orderCreateChestSlotIndex = 49;   // icon rương "tạo order riêng"
    public int orderSearchSignSlotIndex = 48;    // icon sign "search"
    public int orderFilterFunnelSlotIndex = 47;  // icon phễu "filter"

    // ---- Slot index trong GUI /ah — CẦN CALIBRATE LẠI (chưa có ảnh /ah thật) ----
    public int ahSearchSignSlotIndex = 48;
    public int ahFilterFunnelSlotIndex = 47;

    // ---- Slot chứa kết quả gợi ý sau khi gõ tên item vào search (ảnh "Totem of Undying" button) ----
    public int searchSuggestionResultSlotIndex = 1;

    // ---- Slot của item đầu tiên trong danh sách order/ah sau khi search xong ----
    public int firstResultItemSlotIndex = 0;

    /**
     * Số lần click vào icon phễu để đạt filter mong muốn, vì đây là nút "cycle" qua danh sách
     * (Most Per Item / Most Paid / Recently Listed cho /order — set số lần click để dừng ở
     * "Most Paid"; còn /ah tương tự để dừng ở "Lowest Price"). Vì không biết thứ tự cycle thật,
     * để mặc định 0 = không tự bấm, bạn tự xác định qua /autotrade calibrate rồi điền vào đây.
     */
    // Xác nhận từ ảnh chụp: /order có "Most Per Item / Most Paid / Recently Listed";
    // /ah có "Lowest Price / Highest Price / Recently Listed" (3 lựa chọn khác nhau).
    public int orderFilterClicksToMostPaid = 0;
    public int ahFilterClicksToLowestPrice = 0;

    public static AutoTradeConfig load() {
        if (!Files.exists(PATH)) {
            AutoTradeConfig def = new AutoTradeConfig();
            def.items.add(new TradeItemConfig("Totem of Undying", TradeItemConfig.Mode.SELL_AH, 8));
            def.save();
            return def;
        }
        try (Reader r = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            AutoTradeConfig cfg = GSON.fromJson(r, AutoTradeConfig.class);
            return cfg != null ? cfg : new AutoTradeConfig();
        } catch (IOException e) {
            return new AutoTradeConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (IOException ignored) {
        }
    }
}
