package com.thanh.autotrade.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Đọc giá dạng "$89K each", "$1.2M", "150000" ... ra thành số long.
 * Chỉnh regex này nếu format giá server bạn khác (vd dùng "đ", ".", dấu phẩy ngăn nghìn...).
 */
public final class PriceParser {
    private PriceParser() {}

    private static final Pattern PRICE_PATTERN =
            Pattern.compile("\\$?\\s*([0-9]+(?:[.,][0-9]+)?)\\s*([kKmMbB]?)");

    public static Optional<Long> parse(String line) {
        Matcher m = PRICE_PATTERN.matcher(line);
        if (!m.find()) return Optional.empty();
        double base = Double.parseDouble(m.group(1).replace(",", ""));
        String suffix = m.group(2).toLowerCase();
        double multiplier = switch (suffix) {
            case "k" -> 1_000d;
            case "m" -> 1_000_000d;
            case "b" -> 1_000_000_000d;
            default -> 1d;
        };
        return Optional.of(Math.round(base * multiplier));
    }

    /**
     * Trong tooltip có nhiều dòng (tên item, giá, delivered...) — chỉ lấy dòng có ký hiệu "$"
     * để tránh nhầm với dòng "29.8k/30k Delivered" (không có $).
     */
    /** Trung vị của danh sách giá — dùng để chống 1 slot bị troll list giá rác làm lệch kết quả. */
    public static long median(List<Long> values) {
        List<Long> sorted = new java.util.ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    public static Optional<Long> findPriceInTooltip(List<String> lines) {
        for (String line : lines) {
            if (line.contains("$")) {
                Optional<Long> parsed = parse(line);
                if (parsed.isPresent()) return parsed;
            }
        }
        return Optional.empty();
    }
}
