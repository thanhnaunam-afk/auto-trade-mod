package com.thanh.autotrade.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Orders GUI Screen
 * Quản lý multi-orders
 */
public class OrdersScreen extends Screen {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    public OrdersScreen() {
        super(Text.literal("AutoTrade Orders Manager"));
    }

    @Override
    protected void init() {
        // TODO: Implement orders GUI
        // Cần thêm:
        // - List view để hiển thị orders
        // - Fields: Item Name, Quantity, Price/Unit, Buyer Name
        // - Button "Add Order"
        // - Button "Remove Order"
        // - Button "Mark Fulfilled"
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
