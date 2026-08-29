package net.matzified.iceclient.gui;

import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.module.ModuleManager;
import net.matzified.iceclient.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ModuleSettingsScreen extends Screen {

    private final Screen parent;
    private final Module module;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Text.literal(module.getName() + " Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark translucent frosted backdrop
        context.fill(0, 0, width, height, 0xD00A0D14);

        int panelW = Math.min(680, width - 40);
        int panelH = Math.min(500, height - 40);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        // Container Panel
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE111622);
        // Cyan Glow Borders
        context.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF38BDF8);
        context.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, 0x3338BDF8);
        context.fill(panelX, panelY, panelX + 2, panelY + panelH, 0x3338BDF8);
        context.fill(panelX + panelW - 2, panelY, panelX + panelW, panelY + panelH, 0x3338BDF8);

        // Header
        renderHeader(context, panelX, panelY, panelW, mouseX, mouseY);

        // Settings Content List
        renderSettingsList(context, panelX + 20, panelY + 54, panelW - 40, panelH - 70, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext context, int x, int y, int w, int mouseX, int mouseY) {
        // Back button
        int backW = 80;
        int backH = 24;
        int backX = x + 16;
        int backY = y + 12;
        boolean backHover = mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH;
        context.fill(backX, backY, backX + backW, backY + backH, backHover ? 0xFF0284C7 : 0xFF1E293B);
        context.drawTextWithShadow(textRenderer, "← Back", backX + 16, backY + 8, 0xFFFFFFFF);

        // Module Title & Category
        String title = "⚙️ " + module.getName();
        context.drawTextWithShadow(textRenderer, title, backX + backW + 16, y + 18, 0xFF38BDF8);
        String catBadge = module.getCategory().displayName;
        int badgeW = textRenderer.getWidth(catBadge) + 8;
        int badgeX = backX + backW + 20 + textRenderer.getWidth(title);
        context.fill(badgeX, y + 16, badgeX + badgeW, y + 28, 0xFF1E293B);
        context.drawTextWithShadow(textRenderer, catBadge, badgeX + 4, y + 18, 0xFF94A3B8);

        // Toggle Switch
        int switchW = 54;
        int switchH = 24;
        int switchX = x + w - switchW - 20;
        int switchY = y + 12;

        if (module.isEnabled()) {
            context.fill(switchX, switchY, switchX + switchW, switchY + switchH, 0xFF0284C7);
            context.fill(switchX + switchW - 22, switchY + 2, switchX + switchW - 2, switchY + switchH - 2, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "ON", switchX + 8, switchY + 8, 0xFFFFFFFF);
        } else {
            context.fill(switchX, switchY, switchX + switchW, switchY + switchH, 0xFF334155);
            context.fill(switchX + 2, switchY + 2, switchX + 22, switchY + switchH - 2, 0xFF94A3B8);
            context.drawTextWithShadow(textRenderer, "OFF", switchX + 26, switchY + 8, 0xFF94A3B8);
        }

        // Header separator
        context.fill(x, y + 46, x + w, y + 47, 0x2238BDF8);
    }

    private void renderSettingsList(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY) {
        List<Setting<?>> settings = module.getSettings();
        int cardH = 58;
        int gap = 10;
        int totalH = settings.size() * (cardH + gap);
        maxScroll = Math.max(0, totalH - h);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.enableScissor(x, y, x + w, y + h);

        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int sy = y + i * (cardH + gap) - scrollOffset;

            if (sy + cardH < y || sy > y + h) continue;

            boolean isHover = mouseX >= x && mouseX <= x + w && mouseY >= sy && mouseY <= sy + cardH;

            // Setting card background
            context.fill(x, sy, x + w, sy + cardH, isHover ? 0xFF1E2638 : 0xFF141926);
            context.fill(x, sy, x + w, sy + 1, isHover ? 0x4438BDF8 : 0x2238BDF8);
            context.fill(x, sy + cardH - 1, x + w, sy + cardH, 0x2238BDF8);

            // Title & Description
            context.drawTextWithShadow(textRenderer, s.getName(), x + 12, sy + 12, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, s.getDescription(), x + 12, sy + 28, 0xFF94A3B8);

            // Setting Control Widget on right side
            if (s instanceof BooleanSetting) {
                BooleanSetting bs = (BooleanSetting) s;
                int btnW = 50;
                int btnH = 22;
                int btnX = x + w - btnW - 14;
                int btnY = sy + (cardH - btnH) / 2;

                if (bs.getValue()) {
                    context.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF0284C7);
                    context.drawTextWithShadow(textRenderer, "ENABLED", btnX + 6, btnY + 7, 0xFFFFFFFF);
                } else {
                    context.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF334155);
                    context.drawTextWithShadow(textRenderer, "DISABLED", btnX + 4, btnY + 7, 0xFF94A3B8);
                }
            } else if (s instanceof ColorSetting) {
                ColorSetting cs = (ColorSetting) s;
                int swatchX = x + w - (ColorSetting.PALETTE.length * 18) - 10;
                int swatchY = sy + (cardH - 14) / 2;

                for (int col : ColorSetting.PALETTE) {
                    boolean isSelected = cs.getValue() == col;
                    context.fill(swatchX, swatchY, swatchX + 14, swatchY + 14, col);
                    if (isSelected) {
                        context.fill(swatchX - 1, swatchY - 1, swatchX + 15, swatchY, 0xFFFFFFFF);
                        context.fill(swatchX - 1, swatchY + 14, swatchX + 15, swatchY + 15, 0xFFFFFFFF);
                        context.fill(swatchX - 1, swatchY, swatchX, swatchY + 14, 0xFFFFFFFF);
                        context.fill(swatchX + 14, swatchY, swatchX + 15, swatchY + 14, 0xFFFFFFFF);
                    }
                    swatchX += 18;
                }
            } else if (s instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) s;
                int modeW = 120;
                int modeH = 24;
                int modeX = x + w - modeW - 14;
                int modeY = sy + (cardH - modeH) / 2;

                context.fill(modeX, modeY, modeX + modeW, modeY + modeH, 0xFF1E293B);
                context.drawTextWithShadow(textRenderer, "◀", modeX + 6, modeY + 8, 0xFF38BDF8);
                String val = ms.getValue();
                int strW = textRenderer.getWidth(val);
                context.drawTextWithShadow(textRenderer, val, modeX + (modeW - strW) / 2, modeY + 8, 0xFFFFFFFF);
                context.drawTextWithShadow(textRenderer, "▶", modeX + modeW - 12, modeY + 8, 0xFF38BDF8);
            } else if (s instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) s;
                int numW = 100;
                int numH = 24;
                int numX = x + w - numW - 14;
                int numY = sy + (cardH - numH) / 2;

                context.fill(numX, numY, numX + numW, numY + numH, 0xFF1E293B);
                context.drawTextWithShadow(textRenderer, "➖", numX + 8, numY + 8, 0xFF38BDF8);
                String valStr = String.format("%.1fx", ns.getValue());
                int strW = textRenderer.getWidth(valStr);
                context.drawTextWithShadow(textRenderer, valStr, numX + (numW - strW) / 2, numY + 8, 0xFFFFFFFF);
                context.drawTextWithShadow(textRenderer, "➕", numX + numW - 16, numY + 8, 0xFF38BDF8);
            }
        }

        context.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelW = Math.min(680, width - 40);
            int panelH = Math.min(500, height - 40);
            int panelX = (width - panelW) / 2;
            int panelY = (height - panelH) / 2;

            // Back button
            int backW = 80;
            int backH = 24;
            int backX = panelX + 16;
            int backY = panelY + 12;
            if (mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH) {
                close();
                return true;
            }

            // Header Module Toggle
            int switchW = 54;
            int switchH = 24;
            int switchX = panelX + panelW - switchW - 20;
            int switchY = panelY + 12;
            if (mouseX >= switchX && mouseX <= switchX + switchW && mouseY >= switchY && mouseY <= switchY + switchH) {
                module.toggle();
                ModuleManager.getInstance().saveConfig();
                return true;
            }

            // Setting rows
            int listX = panelX + 20;
            int listY = panelY + 54;
            int listW = panelW - 40;
            int listH = panelH - 70;

            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                List<Setting<?>> settings = module.getSettings();
                int cardH = 58;
                int gap = 10;

                for (int i = 0; i < settings.size(); i++) {
                    Setting<?> s = settings.get(i);
                    int sy = listY + i * (cardH + gap) - scrollOffset;

                    if (mouseY >= sy && mouseY <= sy + cardH) {
                        if (s instanceof BooleanSetting) {
                            ((BooleanSetting) s).toggle();
                            ModuleManager.getInstance().saveConfig();
                            return true;
                        } else if (s instanceof ColorSetting) {
                            ColorSetting cs = (ColorSetting) s;
                            int swatchX = listX + listW - (ColorSetting.PALETTE.length * 18) - 10;
                            int swatchY = sy + (cardH - 14) / 2;

                            for (int col : ColorSetting.PALETTE) {
                                if (mouseX >= swatchX && mouseX <= swatchX + 14 && mouseY >= swatchY && mouseY <= swatchY + 14) {
                                    cs.setValue(col);
                                    ModuleManager.getInstance().saveConfig();
                                    return true;
                                }
                                swatchX += 18;
                            }
                        } else if (s instanceof ModeSetting) {
                            ModeSetting ms = (ModeSetting) s;
                            int modeW = 120;
                            int modeH = 24;
                            int modeX = listX + listW - modeW - 14;
                            int modeY = sy + (cardH - modeH) / 2;

                            if (mouseX >= modeX && mouseX <= modeX + 30) {
                                ms.cyclePrev();
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            } else if (mouseX >= modeX + modeW - 30 && mouseX <= modeX + modeW) {
                                ms.cycleNext();
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            }
                        } else if (s instanceof NumberSetting) {
                            NumberSetting ns = (NumberSetting) s;
                            int numW = 100;
                            int numH = 24;
                            int numX = listX + listW - numW - 14;
                            int numY = sy + (cardH - numH) / 2;

                            if (mouseX >= numX && mouseX <= numX + 30) {
                                ns.decrement();
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            } else if (mouseX >= numX + numW - 30 && mouseX <= numX + numW) {
                                ns.increment();
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int) (verticalAmount * 24);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        ModuleManager.getInstance().saveConfig();
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
