package com.thanh.autotrade.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.text.Text;
import com.thanh.autotrade.AutoTradeMod;
import com.thanh.autotrade.safety.SafetyManager;
import com.thanh.autotrade.blacklist.BlacklistManager;

/**
 * AutoTrade Commands
 * /autotrade safety reset - reset safety checks
 * /autotrade blacklist add <player> - thêm vào blacklist
 * /autotrade blacklist remove <player> - xóa khỏi blacklist
 * /autotrade status - hiển thị status
 */
public class AutoTradeCommands {
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("autotrade")
                    .then(ClientCommandManager.literal("safety")
                            .then(ClientCommandManager.literal("reset").executes(ctx -> {
                                SafetyManager safety = AutoTradeMod.getInstance().getSafetyManager();
                                if (safety != null) {
                                    safety.resetSafety();
                                    reply("✅ Safety checks reset");
                                }
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("blacklist")
                            .then(ClientCommandManager.literal("add")
                                    .then(ClientCommandManager.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                                            .executes(ctx -> {
                                                String player = ctx.getArgument("player", String.class);
                                                AutoTradeMod.getInstance().getBlacklistManager().addPlayer(player);
                                                reply("✅ Added " + player + " to blacklist");
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("remove")
                                    .then(ClientCommandManager.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                                            .executes(ctx -> {
                                                String player = ctx.getArgument("player", String.class);
                                                AutoTradeMod.getInstance().getBlacklistManager().removePlayer(player);
                                                reply("✅ Removed " + player + " from blacklist");
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("list").executes(ctx -> {
                                BlacklistManager blacklist = AutoTradeMod.getInstance().getBlacklistManager();
                                reply("Blacklist: " + blacklist.getBlacklist());
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("status").executes(ctx -> {
                        AutoTradeMod mod = AutoTradeMod.getInstance();
                        reply("=== AutoTrade Phase 2 Status ===");
                        reply("Orders: " + mod.getMultiOrderSystem().getAllOrders().size());
                        reply("Stopped Items: " + mod.getSafetyManager().getStoppedItems());
                        reply("Blacklist: " + mod.getBlacklistManager().getBlacklist().size());
                        return 1;
                    }))
            );
        });
    }

    private static void reply(String msg) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[AutoTrade] " + msg), false);
        }
    }
}
