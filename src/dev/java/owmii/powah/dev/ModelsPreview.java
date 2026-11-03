package owmii.powah.dev;

import java.util.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.*;
import net.minecraft.item.ItemStack;
import owmii.powah.Content;

public final class ModelsPreview extends GuiScreen {
    private final int page;
    public ModelsPreview(int page) { this.page = page; }
    @Override public void drawScreen(int x, int y, float partial) {
        drawRect(0, 0, width, height, 0xff111b28); drawCenteredString(fontRenderer, "Powah! — " + (page == 0 ? "Machines" : "Materials and devices"), width / 2, 12, 0x8fe4d3);
        List<String> names = new ArrayList<>();
        for (String name : Content.ITEMS.keySet()) {
            if (page == 0 && (name.endsWith("_basic") || name.endsWith("_nitro")) && Content.BLOCKS.containsKey(name)) names.add(name);
            if (page == 1 && !Content.BLOCKS.containsKey(name)) names.add(name);
        }
        int columns = 7, rows = (names.size() + columns - 1) / columns, cellW = Math.min(85, (width - 24) / columns), cellH = Math.min(62, (height - 35) / rows);
        int left = (width - columns * cellW) / 2;
        for (int i = 0; i < names.size(); i++) {
            int xx = left + i % columns * cellW, yy = 30 + i / columns * cellH;
            GlStateManager.pushMatrix(); GlStateManager.translate(xx + cellW / 2 - 16, yy, 0); GlStateManager.scale(2, 2, 2);
            RenderHelper.enableGUIStandardItemLighting(); itemRender.renderItemAndEffectIntoGUI(Content.stack(names.get(i)), 0, 0); GlStateManager.popMatrix(); RenderHelper.disableStandardItemLighting();
            drawCenteredString(fontRenderer, fontRenderer.trimStringToWidth(names.get(i), cellW - 3), xx + cellW / 2, yy + Math.min(34, cellH - 12), 0xaebed0);
        }
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
