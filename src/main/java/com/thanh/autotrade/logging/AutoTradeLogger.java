package com.thanh.autotrade.logging;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized logging system cho tất cả Phase 2 events
 */
public class AutoTradeLogger {
    private static final Path LOG_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve("autotrade-logs");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public enum LogLevel {
        DEBUG("[DEBUG]"),
        INFO("[INFO]"),
        WARN("[WARN]"),
        ERROR("[ERROR]"),
        SUCCESS("[SUCCESS]");

        final String prefix;
        LogLevel(String prefix) {
            this.prefix = prefix;
        }
    }

    private static final CopyOnWriteArrayList<LogListener> listeners = new CopyOnWriteArrayList<>();

    public interface LogListener {
        void onLog(LogLevel level, String message);
    }

    static {
        try {
            Files.createDirectories(LOG_DIR);
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to create log directory: " + e.getMessage());
        }
    }

    public static void log(LogLevel level, String source, String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String fullMessage = String.format("%s %s [%s] %s", timestamp, level.prefix, source, message);

        // Print to console
        System.out.println("[AutoTrade] " + fullMessage);

        // Write to file
        writeToFile(fullMessage);

        // Notify listeners
        for (LogListener listener : listeners) {
            listener.onLog(level, fullMessage);
        }
    }

    private static void writeToFile(String message) {
        try {
            String date = LocalDateTime.now().format(FILE_DATE_FORMAT);
            Path logFile = LOG_DIR.resolve("autotrade-" + date + ".log");

            Files.write(logFile, (message + "\n").getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to write log: " + e.getMessage());
        }
    }

    public static void info(String source, String message) {
        log(LogLevel.INFO, source, message);
    }

    public static void warn(String source, String message) {
        log(LogLevel.WARN, source, message);
    }

    public static void error(String source, String message) {
        log(LogLevel.ERROR, source, message);
    }

    public static void success(String source, String message) {
        log(LogLevel.SUCCESS, source, message);
    }

    public static void debug(String source, String message) {
        log(LogLevel.DEBUG, source, message);
    }

    public static void addListener(LogListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(LogListener listener) {
        listeners.remove(listener);
    }
}
