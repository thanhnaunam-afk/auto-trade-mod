package com.thanh.autotrade.util;

/**
 * Một bước trong chuỗi thao tác tự động (mở GUI, click slot, gõ text, đọc giá...).
 * run() được gọi đúng 1 lần khi đến lượt. Muốn chờ N tick trước bước kế tiếp thì gọi
 * runner.waitTicks(N) bên trong run().
 */
@FunctionalInterface
public interface ScriptStep {
    void run(ScriptRunner runner);
}
