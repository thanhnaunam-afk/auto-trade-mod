package com.thanh.autotrade.integration;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * Phát hiện màn hình "You Died" và tự động bấm nút Respawn.
 */
public class DeathDetector {
    private static boolean autoRespawnEnabled = true;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof DeathScreen && autoRespawnEnabled) {
                // Tìm nút Respawn trong danh sách button
                for (var child : screen.children()) {
                    if (child instanceof ButtonWidget btn) {
                        String btnText = btn.getMessage().getString().toLowerCase();
                        if (btnText.contains("respawn")) {
                            // Bấm nút Respawn sau 1 tick (để tránh race condition)
                            MinecraftClient.getInstance().execute(() -> btn.onPress());
                            return;
                        }
                    }
                }
            }
        });
    }

    public static void setAutoRespawnEnabled(boolean enabled) {
        autoRespawnEnabled = enabled;
    }

    public static boolean isAutoRespawnEnabled() {
        return autoRespawnEnabled;
    }
}
