package com.thanh.autotrade.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hàng đợi các ScriptStep, chạy mỗi tick 1 bước (có delay).
 * Gọi push(...) để nối thêm bước vào cuối, pushFirst(...) để chèn bước vào ngay đầu
 * hàng đợi (dùng khi 1 bước cần "sinh ra" các bước con phụ thuộc dữ liệu vừa đọc được,
 * ví dụ sau khi đọc giá /ah xong mới biết cần gõ giá bao nhiêu).
 *
 * Đây KHÔNG tự chạy theo ClientTickEvents — AutoTradeMod gọi runner.tick() mỗi client tick.
 */
public class ScriptRunner {
    private final Deque<ScriptStep> queue = new ArrayDeque<>();
    private int waitTicks = 0;
    private boolean errored = false;
    private String lastError = null;

    public void push(ScriptStep step) {
        queue.addLast(step);
    }

    public void pushFirst(ScriptStep step) {
        queue.addFirst(step);
    }

    /** Gọi bên trong 1 step để bảo runner chờ N tick trước khi chạy step kế tiếp. */
    public void waitTicks(int ticks) {
        this.waitTicks = Math.max(this.waitTicks, ticks);
    }

    public void fail(String reason) {
        this.errored = true;
        this.lastError = reason;
        this.queue.clear();
    }

    public boolean isErrored() {
        return errored;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isIdle() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
        waitTicks = 0;
    }

    public void tick() {
        if (errored) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        ScriptStep step = queue.pollFirst();
        if (step == null) return;
        try {
            step.run(this);
        } catch (Exception e) {
            fail("Exception trong script step: " + e);
        }
    }
}
