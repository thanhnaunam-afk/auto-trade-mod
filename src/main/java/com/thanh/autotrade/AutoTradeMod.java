package com.thanh.autotrade;

import com.thanh.autotrade.config.AutoTradeConfig;
import com.thanh.autotrade.gui.AutoTradeMenuScreen;
import com.thanh.autotrade.trade.TradeStateMachine;
import com.thanh.autotrade.util.ChatBuffer;
import com.thanh.autotrade.util.ScreenUtil;
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
 * Mở menu chính bằng phím "]" (đổi được trong Options -> Controls -> AutoTrade).
 *
 * Calibrate dùng PHÍM TẮT RIÊNG (mặc định "[") thay vì lệnh chat, vì mở chat
 * sẽ đóng luôn GUI /order hoặc /ah đang mở — dùng phím tắt thì không mở màn
 * hình nào cả nên GUI vẫn đứng yên trong lúc đọc slot.
 *
 * chatdump vẫn là lệnh chat vì lúc dùng (đọc /balance) không có GUI nào đang mở.
 */
public class AutoTradeMod implements ClientModInitializer {
    private static AutoTradeMod instance;

    private static final KeyBinding.Category AUTOTRADE_CATEGORY =
            KeyBinding.Category.create(Identifier.of("autotrade", "keybinds"));

    private AutoTradeConfig config;
    private TradeStateMachine stateMachine;
    private KeyBinding openMenuKey;
    private KeyBinding calibrateKey;

    public static AutoTradeMod getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        config = AutoTradeConfig.load();
        stateMachine = new TradeStateMachine(config);
        ChatBuffer.init();

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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (stateMachine != null) stateMachine.tick();
            while (openMenuKey.wasPressed()) {
                openMenu();
            }
            while (calibrateKey.wasPressed()) {
                calibrateCurrentScreen();
            }
        });

        // chatdump giữ dạng lệnh chat (client-side, KHÔNG gửi lên server) vì lúc
        // dùng để đọc /balance thì không có GUI nào đang mở.
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
    }

    public void openMenu() {
        MinecraftClient.getInstance().setScreen(new AutoTradeMenuScreen(config, stateMachine, this));
    }

    public void reloadConfig() {
        config = AutoTradeConfig.load();
        stateMachine = new TradeStateMachine(config);
        openMenu(); // mở lại menu để hiển thị dữ liệu mới ngay
    }

    /**
     * Đứng trong /order hoặc /ah, bấm phím "[" (đổi được trong Options -> Controls):
     * mod in ra chat toàn bộ index + tên item + dòng tooltip đầu tiên của TỪNG slot
     * trong GUI đang mở, để bạn đối chiếu với ảnh và điền đúng số vào autotrade.json.
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
}
