package com.thanh.autotrade.util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectScreen;
import net.minecraft.text.Text;

/**
 * Tự động reconnect sau X giây khi bị disconnect.
 */
public class ReconnectManager {
    private static int reconnectCountdown = 0;
    private static boolean autoReconnectEnabled = false;
    private static int delaySeconds = 10;

    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (autoReconnectEnabled) {
                reconnectCountdown = delaySeconds * 20; // convert to ticks
                showReconnectScreen();
            }
        });
    }

    public static void tick() {
        if (reconnectCountdown > 0) {
            reconnectCountdown--;
            if (reconnectCountdown == 0) {
                attemptReconnect();
            }
        }
    }

    private static void showReconnectScreen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.setScreen(new DisconnectScreen(
                    Text.literal("Disconnected"),
                    Text.literal("AutoReconnect trong " + delaySeconds + "s...")
            ));
        }
    }

    private static void attemptReconnect() {
        MinecraftClient mc = MinecraftClient.getInstance();
        // TODO: thực hiện reconnect (cần server address từ config hoặc lưu trữ)
        // Tạm để placeholder — cần integrate với server join logic của Minecraft
    }

    public static void setAutoReconnect(boolean enabled, int delaySec) {
        autoReconnectEnabled = enabled;
        delaySeconds = delaySec;
    }

    public static boolean isAutoReconnecting() {
        return reconnectCountdown > 0;
    }
}
