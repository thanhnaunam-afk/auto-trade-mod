package com.thanh.autotrade;

import com.thanh.autotrade.config.AutoTradeConfig;
import com.thanh.autotrade.gui.AutoTradeMenuScreen;
import com.thanh.autotrade.trade.TradeStateMachine;
import com.thanh.autotrade.util.ChatBuffer;
import com.thanh.autotrade.util.ScreenUtil;

import com.thanh.autotrade.ai.GeminiMultiAccountAnalyzer;
import com.thanh.autotrade.blacklist.BlacklistManager;
import com.thanh.autotrade.config.AdvancedConfig;
import com.thanh.autotrade.evade.EvadeDetectionV2;
import com.thanh.autotrade.integration.DiscordWebhookManager;
import com.thanh.autotrade.safety.BalanceChecker;
import com.thanh.autotrade.safety.SafetyManager;
import com.thanh.autotrade.trade.MultiOrderSystem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * AutoTrade Mod Phase 2
 * Integrates: Evade Detection, Multi-Order, Safety, Balance Check, Gemini AI, Blacklist
 * 
 * Keybinds:
 * - "]" (RIGHT_BRACKET): Open main menu
 * - "[" (LEFT_BRACKET): Calibrate current screen
 */
public class AutoTradeMod implements ClientModInitializer {
    private static AutoTradeMod instance;

    private static final KeyBinding.Category AUTOTRADE_CATEGORY =
            KeyBinding.Category.create(Identifier.of("autotrade", "keybinds"));

    // Phase 1 systems
    private AutoTradeConfig config;
    private TradeStateMachine stateMachine;
    private KeyBinding openMenuKey;
    private KeyBinding calibrateKey;

    // Phase 2 systems
    private MultiOrderSystem multiOrderSystem;
    private BalanceChecker balanceChecker;
    private SafetyManager safetyManager;
    private BlacklistManager blacklistManager;
    private DiscordWebhookManager discordWebhook;
    private GeminiMultiAccountAnalyzer geminiAnalyzer;
    private AdvancedConfig advancedConfig;

    public static AutoTradeMod getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        
        // Load Phase 1 config
        config = AutoTradeConfig.load();
        stateMachine = new TradeStateMachine(config);
        ChatBuffer.init();

        // Load Phase 2 config
        try {
            advancedConfig = new AdvancedConfig();
            advancedConfig.load();
            
            // Initialize Phase 2 systems
            initializePhase2Systems();
        } catch (Exception e) {
            System.err.println("[AutoTrade] Failed to load Phase 2: " + e.getMessage());
            e.printStackTrace();
        }

        // Register keybindings
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autotrade.openmenu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                AUTOTRADE_CATEGORY
        ));

        calibrateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autotrade.calibrate",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                AUTOTRADE_CATEGORY
        ));

        // Tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (stateMachine != null) stateMachine.tick();
            
            // Phase 2 ticks
            if (multiOrderSystem != null) multiOrderSystem.tick();
            if (balanceChecker != null) balanceChecker.tick();
            if (geminiAnalyzer != null) geminiAnalyzer.tick();
            
            while (openMenuKey.wasPressed()) {
                openMenu();
            }
            while (calibrateKey.wasPressed()) {
                calibrateCurrentScreen();
            }
        });

        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("autotrade")
                    .then(ClientCommandManager.literal("chatdump").executes(ctx -> {
                        reply("20 dong chat gan nhat (moi nhat o duoi):");
                        for (String line : ChatBuffer.snapshot()) {
                            reply("  " + line);
                        }
                        return 1;
                    }))
            );
        });

        reply("✅ AutoTrade Phase 2 loaded!");
    }

    private void initializePhase2Systems() {
        // Discord webhook
        discordWebhook = new DiscordWebhookManager(advancedConfig.discordWebhookUrl);

        // Multi-order system
        multiOrderSystem = new MultiOrderSystem();
        multiOrderSystem.loadOrders();

        // Balance checker
        balanceChecker = new BalanceChecker(discordWebhook);

        // Safety manager
        safetyManager = new SafetyManager(multiOrderSystem, discordWebhook);

        // Blacklist
        blacklistManager = new BlacklistManager();

        // Gemini analyzer
        geminiAnalyzer = new GeminiMultiAccountAnalyzer(advancedConfig, "MainAccount");

        // Evade detection
        EvadeDetectionV2.init();

        if (discordWebhook != null) {
            discordWebhook.logEvent("SUCCESS", "🚀 **AutoTrade Phase 2 initialized!**");
        }
    }

    public void openMenu() {
        MinecraftClient.getInstance().setScreen(new AutoTradeMenuScreen(config, stateMachine, this));
    }

    public void reloadConfig() {
        config = AutoTradeConfig.load();
        stateMachine = new TradeStateMachine(config);
        openMenu();
    }

    /**
     * Calibrate: bấm "[" trong /order hoặc /ah GUI
     */
    private void calibrateCurrentScreen() {
        HandledScreen<?> screen = ScreenUtil.currentHandledScreen();
        if (screen == null) {
            reply("Không có GUI nào đang mở (không phải HandledScreen).");
            return;
        }
        reply("Title: '" + screen.getTitle().getString() + "' — " + screen.getScreenHandler().slots.size() + " slot:");
        int slotCount = screen.getScreenHandler().slots.size();
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = ScreenUtil.getSlotStack(i);
            if (stack.isEmpty()) continue;
            List<String> lines = ScreenUtil.getSlotTooltipLines(i);
            String firstLine = lines.isEmpty() ? "" : lines.get(0);
            reply("  [" + i + "] " + stack.getItem() + " x" + stack.getCount() + " — " + firstLine);
        }
    }

    private void reply(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[AutoTrade] " + msg), false);
        }
    }

    // Getters for Phase 2 systems
    public MultiOrderSystem getMultiOrderSystem() {
        return multiOrderSystem;
    }

    public BalanceChecker getBalanceChecker() {
        return balanceChecker;
    }

    public SafetyManager getSafetyManager() {
        return safetyManager;
    }

    public BlacklistManager getBlacklistManager() {
        return blacklistManager;
    }

    public DiscordWebhookManager getDiscordWebhook() {
        return discordWebhook;
    }

    public GeminiMultiAccountAnalyzer getGeminiAnalyzer() {
        return geminiAnalyzer;
    }
}
