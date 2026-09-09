package com.thanh.autotrade.gui;

import com.thanh.autotrade.config.AdvancedConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Màn hình settings: nhập Discord webhook URL, Gemini API key, tên account.
 */
public class SettingsScreen extends Screen {
    private final Screen parent;
    private final AdvancedConfig config;

    private TextFieldWidget webhookUrlField;
    private TextFieldWidget geminiKeyField;
    private TextFieldWidget accountNameField;

    public SettingsScreen(Screen parent, AdvancedConfig config) {
        super(Text.literal("AutoTrade Settings"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = 40;
        int fieldWidth = 250;

        // Webhook URL
        this.addDrawableChild(new TextFieldWidget(this.textRenderer, centerX - fieldWidth / 2, top, fieldWidth, 20,
                Text.literal("Webhook URL")));
        webhookUrlField = (TextFieldWidget) this.children().get(this.children().size() - 1);
        webhookUrlField.setText(config.discordWebhookUrl);

        // Gemini API Key
        this.addDrawableChild(new TextFieldWidget(this.textRenderer, centerX - fieldWidth / 2, top + 30, fieldWidth, 20,
                Text.literal("Gemini API Key")));
        geminiKeyField = (TextFieldWidget) this.children().get(this.children().size() - 1);
        geminiKeyField.setText(config.geminiApiKey);

        // Account Name
        this.addDrawableChild(new TextFieldWidget(this.textRenderer, centerX - fieldWidth / 2, top + 60, fieldWidth, 20,
                Text.literal("Account Name")));
        accountNameField = (TextFieldWidget) this.children().get(this.children().size() - 1);
        accountNameField.setText(config.accountName);

        // Save button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("💾 Lưu"), btn -> {
            config.discordWebhookUrl = webhookUrlField.getText();
            config.geminiApiKey = geminiKeyField.getText();
            config.accountName = accountNameField.getText();
            config.save();
            this.client.setScreen(parent);
        }).dimensions(centerX - 60, top + 100, 120, 20).build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("❌ Huỷ"), btn -> {
            this.client.setScreen(parent);
        }).dimensions(centerX - 60, top + 125, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Discord Webhook URL:"), this.width / 2 - 120, 35, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Gemini API Key:"), this.width / 2 - 120, 65, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Account Name:"), this.width / 2 - 120, 95, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
