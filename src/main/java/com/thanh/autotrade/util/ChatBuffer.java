package com.thanh.autotrade.util;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Server không có API để đọc số dư trực tiếp — cách duy nhất là gửi lệnh "balance"
 * rồi đọc dòng phản hồi trong chat. Lớp này giữ lại N dòng chat gần nhất để
 * TradeStateMachine soi tìm dòng có "$" sau khi gửi lệnh.
 */
public final class ChatBuffer {
    private static final List<String> recent = new ArrayList<>();
    private static final int MAX_LINES = 25;

    private ChatBuffer() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            synchronized (recent) {
                recent.add(message.getString());
                if (recent.size() > MAX_LINES) recent.remove(0);
            }
        });
    }

    /** Bản sao các dòng gần nhất, mới nhất ở cuối danh sách. */
    public static List<String> snapshot() {
        synchronized (recent) {
            return new ArrayList<>(recent);
        }
    }

    public static List<String> snapshotNewestFirst() {
        List<String> copy = snapshot();
        Collections.reverse(copy);
        return copy;
    }
}
