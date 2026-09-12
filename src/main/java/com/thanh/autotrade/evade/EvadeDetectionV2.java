package com.thanh.autotrade.evade;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phát hiện player trong 10 blocks → Random behavior (nhún, xoay, đập block)
 * Bị cùng 1 người phát hiện 2 lần → /pay froglighter + /rtp
 * Restart sau 1h
 */
public class EvadeDetectionV2 {
    private static final double DETECTION_RADIUS = 10.0;
    private static final int DETECTION_COOLDOWN_TICKS = 1200; // 1 phút
    private static final int AUTO_RESTART_TICKS = 72000; // 1 giờ

    private static final Map<String, Integer> detectionCount = new ConcurrentHashMap<>();
    private static int lastDetectionTick = 0;
    private static int detectionCountdown = 0;
    private static boolean isEvading = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Check detect mỗi tick
            checkNearbyPlayers(client);

            // Countdown
            if (detectionCountdown > 0) {
                detectionCountdown--;
                if (detectionCountdown == 0) {
                    isEvading = false;
                }
            }

            // Auto restart sau 1h
            if (lastDetectionTick > 0) {
                lastDetectionTick++;
                if (lastDetectionTick >= AUTO_RESTART_TICKS) {
                    autoRestart();
                }
            }
        });
    }

    private static void checkNearbyPlayers(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        Vec3d playerPos = client.player.getPos();
        String detectedPlayer = null;

        // Scan players trong vòng 10 blocks
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != client.player) {
                double distance = playerPos.distanceTo(entity.getPos());
                if (distance <= DETECTION_RADIUS) {
                    detectedPlayer = entity.getName().getString();
                    break;
                }
            }
        }

        // Phát hiện
        if (detectedPlayer != null) {
            onPlayerDetected(detectedPlayer, client);
        }
    }

    private static void onPlayerDetected(String playerName, MinecraftClient client) {
        // Cooldown check
        long currentTick = System.currentTimeMillis() / 50; // ~1 tick = 50ms
        if (currentTick - lastDetectionTick < DETECTION_COOLDOWN_TICKS) {
            return;
        }

        lastDetectionTick = (int) currentTick;
        int count = detectionCount.getOrDefault(playerName, 0) + 1;
        detectionCount.put(playerName, count);

        if (count >= 2) {
            // Lần 2: pay + rtp
            onPaymentTriggered(playerName, client);
        } else {
            // Lần 1: random evade
            randomEvade(client);
            detectionCountdown = 600; // 30 giây
            isEvading = true;
        }
    }

    private static void randomEvade(MinecraftClient client) {
        if (client.player == null) return;

        Random rand = new Random();
        int action = rand.nextInt(3);

        switch (action) {
            case 0: // Jump
                jumpRandom(client, rand.nextInt(3) + 1);
                break;
            case 1: // Rotate
                rotateRandom(client, rand);
                break;
            case 2: // Break block
                breakRandomBlock(client, rand);
                break;
        }
    }

    private static void jumpRandom(MinecraftClient client, int times) {
        for (int i = 0; i < times; i++) {
            client.player.jump();
            try {
                Thread.sleep(100 + new Random().nextInt(200));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void rotateRandom(MinecraftClient client, Random rand) {
        float randomYaw = rand.nextFloat() * 360;
        float randomPitch = (rand.nextFloat() - 0.5f) * 100;
        client.player.setYaw(randomYaw);
        client.player.setPitch(randomPitch);
    }

    private static void breakRandomBlock(MinecraftClient client, Random rand) {
        // TODO: Thêm logic phá block gần đó (cần interact manager)
    }

    private static void onPaymentTriggered(String playerName, MinecraftClient client) {
        if (client.player == null) return;

        // Gửi command /pay froglighter <all money>
        String command = "/pay froglighter <all money>";
        client.player.networkHandler.sendChatCommand("pay froglighter " + client.player.getInventory().getMains()
                .stream().mapToLong(stack -> stack.getCount()).sum());

        // Wait 2 giây rồi /rtp
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        client.player.networkHandler.sendChatCommand("rtp");

        // Reset
        detectionCount.clear();
        lastDetectionTick = 0;
    }

    private static void autoRestart() {
        detectionCount.clear();
        lastDetectionTick = 0;
    }

    public static boolean isEvading() {
        return isEvading;
    }
}
