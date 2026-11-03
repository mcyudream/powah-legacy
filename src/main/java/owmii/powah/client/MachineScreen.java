package owmii.powah.client;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.fluids.FluidStack;
import owmii.powah.block.*;
import owmii.powah.inventory.*;
import owmii.powah.item.ItemEnergy;
import owmii.powah.network.Controls;

/** Legacy rendering of the pinned upstream Powah screens, using their original atlases. */
public final class MachineScreen extends GuiContainer {
    private static final ResourceLocation WIDGETS = texture("lollipop", "button_ov");
    private final MachineTile tile;
    private final MachineLayout layout;
    private final EnumFacing cableSide;
    private final ResourceLocation background;

    public MachineScreen(InventoryPlayer player, MachineTile tile) { this(player, tile, EnumFacing.NORTH); }
    public MachineScreen(InventoryPlayer player, MachineTile tile, EnumFacing cableSide) {
        super(new MachineContainer(player, tile, cableSide));
        this.tile = tile;
        this.layout = ((MachineContainer) inventorySlots).layout;
        this.cableSide = cableSide;
        this.background = texture("powah", layout.texture);
        xSize = layout.width; ySize = layout.height;
    }
    private static ResourceLocation texture(String namespace, String name) {
        return new ResourceLocation(namespace, "textures/gui/container/" + name + ".png");
    }
    @Override public void initGui() {
        super.initGui();
        guiTop = Math.max(14, Math.min(guiTop, height - ySize));
        if (layout == MachineLayout.CABLE) {
            icon(cableSide.ordinal(), 5, 5, 18, 19, () -> new Region(background, 153 + 18 * cableMode(), 0), () -> sideTooltip(cableSide.ordinal()));
            icon(6, xSize - 19, 10, 9, 8, this::redstoneIcon, () -> List.of(I18n.format("powah.redstone." + tile.redstoneMode)));
        } else {
            int[][] offsets = {{8, 20}, {8, 8}, {8, 14}, {14, 20}, {2, 14}, {14, 14}};
            for (int i = 0; i < 6; i++) {
                final int side = i;
                icon(i, xSize + offsets[i][0], offsets[i][1], 5, 5,
                    () -> new Region(WIDGETS, new int[]{28, 38, 33, 43}[tile.sides[side]], 16), () -> sideTooltip(side));
            }
            icon(Controls.ALL_SIDES, xSize + 14, 8, 5, 5, () -> new Region(WIDGETS, 23, 16),
                () -> List.of(I18n.format("info.lollipop.facing") + ": " + I18n.format("info.lollipop.all"), I18n.format("powah.side." + tile.sides[1])));
            icon(6, xSize + 2, 34, 9, 8, this::redstoneIcon, () -> List.of(I18n.format("powah.redstone." + tile.redstoneMode)));
        }
        if (tile.isEnder()) for (int i = 0; i < tile.tier().channels; i++) {
            final int channel = i;
            icon(Controls.CHANNEL_BASE + i, 5 + i * 14, 55, 12, 12,
                () -> new Region(background, tile.channel == channel ? 176 : 188, 0), () -> List.of(I18n.format("powah.channel", channel + 1)));
        }
        if (tile instanceof ReactorTile reactor) {
            icon(8, -11, 10, 8, 8, () -> new Region(background, 224, reactor.autoMode ? 8 : 0), () -> {
                List<String> lines = new ArrayList<>(); lines.add(I18n.format("powah.auto." + reactor.autoMode));
                for (String paragraph : I18n.format("info.powah.gen.mode.desc").replace("\\n", "\n").split("\n"))
                    lines.addAll(fontRenderer.listFormattedStringToWidth(paragraph, 220));
                return lines;
            });
        }
    }
    private int cableMode() { return new int[]{0, 2, 1, 3}[tile.sides[cableSide.ordinal()]]; }
    private Region redstoneIcon() { return new Region(WIDGETS, tile.redstoneMode == 2 ? 47 : 38, tile.redstoneMode == 1 ? 8 : 0); }
    private List<String> sideTooltip(int side) {
        return List.of(I18n.format("info.lollipop.facing") + ": " + sideName(side), I18n.format("powah.side." + tile.sides[side]));
    }
    private static String sideName(int side) { return I18n.format("info.lollipop.side." + EnumFacing.byIndex(side).getName()); }
    private void icon(int action, int x, int y, int width, int height, Supplier<Region> sprite, Supplier<List<String>> tooltip) {
        buttonList.add(new Icon(action, guiLeft + x, guiTop + y, width, height, sprite, tooltip));
    }
    @Override protected void actionPerformed(GuiButton button) { Controls.CHANNEL.sendToServer(new Controls(tile.getPos(), button.id)); }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        // Consume external widget clicks before vanilla's item-drop handling.
        if (button == 0) for (GuiButton widget : buttonList) if (widget.mousePressed(mc, x, y)) {
            widget.playPressSound(mc.getSoundHandler()); actionPerformed(widget); return;
        }
        if ((button == 0 || button == 1) && hasTank() && over(x, y, 157, 5, 14, 65)) {
            Controls.CHANNEL.sendToServer(new Controls(tile.getPos(), button == 0 ? Controls.TANK : Controls.TANK_DRAIN)); return;
        }
        super.mouseClicked(x, y, button);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        blit(background, 0, 0, 0, 0, xSize, ySize);
        if (layout == MachineLayout.CABLE) { blit(WIDGETS, xSize - 21, 6, 23, 0, 15, 16); return; }
        blit(WIDGETS, xSize, 4, 0, 0, 23, 25);
        blit(WIDGETS, xSize, 30, 23, 0, 15, 16);
        double energy = tile.tier() == Tier.CREATIVE ? 1 : ratio(tile.energy().stored(), tile.energy().capacity());
        switch (layout) {
            case ENERGY_CELL, TRANSMITTER, ENDER -> horizontal(background, 31, 6, 0, ySize, 139, 37, energy);
            case WIDE, DISCHARGER -> horizontal(background, 6, 6, 0, ySize, 164, 37, energy);
            default -> vertical(layout == MachineLayout.MAGMATOR ? texture("powah", "furnator") : background, 5, 5, 176, 0, 14, 39, energy);
        }
        if (layout == MachineLayout.FURNATOR) {
            vertical(background, 110, 18, 190, 0, 4, 16, ratio(tile.fuelBuffer, tile.fuelCapacity));
            blitAlpha(background, 94, 43, 176, 39, 11, 18, tile.active ? 1 : 0);
        } else if (layout == MachineLayout.MAGMATOR) {
            blitAlpha(background, 83, 29, 176, 39, 11, 18, tile.active ? 1 : 0);
        } else if (tile instanceof ReactorTile reactor) {
            vertical(background, 103, 13, 190, 16, 5, 48, ratio(reactor.fuel, 1000));
            vertical(background, 51, 6, 190, 0, 5, 16, ratio(reactor.carbon, reactor.carbonCapacity));
            vertical(background, 51, 52, 195, 0, 5, 16, ratio(reactor.redstone, reactor.redstoneCapacity));
            vertical(background, 140, 52, 200, 0, 5, 16, ratio(reactor.coolant, reactor.coolantCapacity));
            vertical(background, 114, 28, 205, 0, 4, 18, ratio(reactor.temperature, 1000));
            blit(background, -15, 6, 209, 0, 15, 16);
        } else if (layout == MachineLayout.TRANSMITTER && !tile.inventory.getStackInSlot(0).isEmpty()) {
            blit(background, 9, 5, 176, 0, 6, 6);
        }
        if (hasTank()) drawTank();
        if (tile.kind().equals("ender_cell") && isShiftKeyDown()) for (Slot slot : inventorySlots.inventorySlots) {
            if (slot.inventory == mc.player.inventory && ItemEnergy.extendable(slot.getStack()))
                blit(texture("lollipop", "misc"), slot.xPos, slot.yPos, 0, 0, 16, 16);
        }
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = new ItemStack(tile.getBlockType()).getDisplayName();
        fontRenderer.drawStringWithShadow(name, (xSize - fontRenderer.getStringWidth(name)) / 2f, -14, 0x999999);
        if (layout == MachineLayout.CABLE) {
            String side = sideName(cableSide.ordinal());
            fontRenderer.drawString(side, (xSize - fontRenderer.getStringWidth(side)) / 2, 10, 0x555555);
        } else if (horizontalGauge()) {
            int x = layout == MachineLayout.WIDE || layout == MachineLayout.DISCHARGER ? 12 : 38;
            GlStateManager.enableBlend();
            String energy = tile.tier() == Tier.CREATIVE ? I18n.format("info.powah.unlimited") : commas(tile.energy().stored()) + "/" + compact(tile.energy().capacity()) + " FE";
            if (fontRenderer.getStringWidth(energy) > xSize - x - 8)
                energy = compact(tile.energy().stored()) + "/" + compact(tile.energy().capacity()) + " FE";
            int color = layout == MachineLayout.ENDER ? 0x724affde : 0x66000000;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(energy, xSize - x - 8), x, 13, color);
            fontRenderer.drawString(compact(tile.tier().transfer(tile.kind())) + " FE/t", x, 27, color);
            GlStateManager.disableBlend();
        } else if (layout == MachineLayout.THERMO) {
            long base = tile.tier().production(tile.kind());
            fontRenderer.drawString((base == 0 ? 0 : 100 * tile.generating / base) + "% (" + tile.generating + " FE/t)", 34, 10, 0x555555);
        }
        if (tile.isEnder()) {
            GlStateManager.pushMatrix(); GlStateManager.scale(.5f, .5f, 1); GlStateManager.enableBlend();
            for (int i = 0; i < 12; i++) {
                String number = Integer.toString(i + 1);
                fontRenderer.drawString(number, 10 + i * 28 + (24 - fontRenderer.getStringWidth(number)) / 2, 119,
                    i < tile.tier().channels ? 0xff3e8087 : 0x723e8087);
            }
            GlStateManager.disableBlend(); GlStateManager.popMatrix();
        }
    }
    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground(); super.drawScreen(x, y, partial); renderHoveredToolTip(x, y);
        for (GuiButton button : buttonList) if (button.isMouseOver()) { drawHoveringText(((Icon) button).tooltip.get(), x, y); return; }
        int gaugeX = layout == MachineLayout.WIDE || layout == MachineLayout.DISCHARGER ? 6 : 31;
        int gaugeWidth = gaugeX == 6 ? 164 : 139;
        if (layout != MachineLayout.CABLE && (horizontalGauge() ? over(x, y, gaugeX, 6, gaugeWidth, 37) : over(x, y, 5, 5, 14, 39))) {
            List<String> lines = new ArrayList<>();
            lines.add(I18n.format("info.lollipop.stored") + ": §8" + commas(tile.energy().stored()) + "/" + compact(tile.energy().capacity()) + " FE");
            if (Set.of("furnator", "magmator", "thermo_generator", "solar_panel", "reactor").contains(tile.kind()))
                lines.add(I18n.format("info.lollipop.generating") + ": §8" + compact(tile.generating) + " FE/t");
            lines.add(I18n.format("info.lollipop.max.extract") + ": §8" + compact(tile.tier().transfer(tile.kind())) + " FE/t");
            if (tile.kind().equals("ender_cell")) lines.add(I18n.format("powah.ui.ender_extend"));
            drawHoveringText(lines, x, y);
        } else if (hasTank() && over(x, y, 157, 5, 14, 65)) {
            FluidStack fluid = tile.tank.getFluid();
            drawHoveringText(List.of(fluid == null ? I18n.format("info.lollipop.fluid") + ": ---" : fluid.getLocalizedName(),
                commas(tile.tank.getFluidAmount()) + "/" + commas(tile.tank.getCapacity()) + " mB", I18n.format("powah.ui.tank")), x, y);
        } else if (tile instanceof ReactorTile reactor) {
            if (over(x, y, 103, 13, 5, 48)) gaugeTooltip("item.powah.uraninite.name", reactor.fuel, 1000, x, y);
            else if (over(x, y, 51, 6, 5, 16)) gaugeTooltip("info.powah.carbon", reactor.carbon, reactor.carbonCapacity, x, y);
            else if (over(x, y, 51, 52, 5, 16)) gaugeTooltip("info.powah.redstone", reactor.redstone, reactor.redstoneCapacity, x, y);
            else if (over(x, y, 140, 52, 5, 16)) drawHoveringText(List.of(I18n.format("info.powah.solid.coolant"),
                commas(reactor.coolant) + "/" + commas(reactor.coolantCapacity), reactor.coolantTemperature + " °C"), x, y);
            else if (over(x, y, 114, 28, 4, 18)) drawHoveringText(List.of(reactor.temperature + " °C"), x, y);
        } else if (layout == MachineLayout.FURNATOR && over(x, y, 110, 18, 4, 16)) {
            drawHoveringText(List.of(I18n.format("info.powah.carbon"), commas(tile.fuelBuffer) + "/" + commas(tile.fuelCapacity) + " FE"), x, y);
        }
    }
    private void gaugeTooltip(String key, double amount, double capacity, int x, int y) {
        drawHoveringText(List.of(I18n.format(key), String.format(Locale.ROOT, "%.1f/%.1f", amount, capacity)), x, y);
    }
    private boolean hasTank() { return layout == MachineLayout.MAGMATOR || layout == MachineLayout.THERMO || layout == MachineLayout.REACTOR; }
    private boolean horizontalGauge() { return layout == MachineLayout.ENERGY_CELL || layout == MachineLayout.ENDER || layout == MachineLayout.TRANSMITTER || layout == MachineLayout.WIDE || layout == MachineLayout.DISCHARGER; }
    private boolean over(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= guiLeft + x && mouseY >= guiTop + y && mouseX < guiLeft + x + width && mouseY < guiTop + y + height;
    }
    private static double ratio(double value, double capacity) { return capacity <= 0 ? 0 : Math.max(0, Math.min(1, value / capacity)); }
    private static String commas(long value) { return String.format(Locale.ROOT, "%,d", value); }
    private static String compact(long value) {
        long unit = 1; int suffix = 0;
        while (value / unit >= 1000 && suffix < 6) { unit *= 1000; suffix++; }
        if (suffix == 0) return Long.toString(value);
        long tenths = value / (unit / 10);
        return (tenths < 100 && tenths % 10 != 0 ? String.format(Locale.ROOT, "%.1f", tenths / 10.0) : Long.toString(tenths / 10)) + "kMGTPE".charAt(suffix - 1);
    }
    private void horizontal(ResourceLocation sheet, int x, int y, int u, int v, int width, int height, double fraction) {
        blit(sheet, x, y, u, v, (int) (width * fraction), height);
    }
    private void vertical(ResourceLocation sheet, int x, int y, int u, int v, int width, int height, double fraction) {
        int fill = (int) (height * fraction); blit(sheet, x, y + height - fill, u, v + height - fill, width, fill);
    }
    private void blit(ResourceLocation sheet, int x, int y, int u, int v, int width, int height) { blitAlpha(sheet, x, y, u, v, width, height, 1); }
    private void blitAlpha(ResourceLocation sheet, int x, int y, int u, int v, int width, int height, float alpha) {
        drawTexture(mc, sheet, guiLeft + x, guiTop + y, u, v, width, height, alpha);
    }
    private static void drawTexture(Minecraft mc, ResourceLocation sheet, int x, int y, int u, int v, int width, int height, float alpha) {
        if (width <= 0 || height <= 0 || alpha <= 0) return;
        GlStateManager.color(1, 1, 1, alpha); GlStateManager.enableBlend(); mc.getTextureManager().bindTexture(sheet);
        drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, 256, 256);
        GlStateManager.disableBlend(); GlStateManager.color(1, 1, 1, 1);
    }
    private void drawTank() {
        FluidStack fluid = tile.tank.getFluid(); if (fluid == null || fluid.amount == 0) return;
        var sprite = mc.getTextureMapBlocks().getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
        int color = fluid.getFluid().getColor(fluid), fill = (int) (65 * ratio(fluid.amount, tile.tank.getCapacity()));
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
        for (int offset = 0; offset < fill; offset += 16) {
            int height = Math.min(16, fill - offset), x = guiLeft + 157, y = guiTop + 70 - offset - height;
            double u0 = sprite.getMinU(), u1 = sprite.getInterpolatedU(14), v0 = sprite.getMinV(), v1 = sprite.getInterpolatedV(height);
            var buffer = Tessellator.getInstance().getBuffer(); buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(x, y + height, 0).tex(u0, v1).endVertex(); buffer.pos(x + 14, y + height, 0).tex(u1, v1).endVertex();
            buffer.pos(x + 14, y, 0).tex(u1, v0).endVertex(); buffer.pos(x, y, 0).tex(u0, v0).endVertex(); Tessellator.getInstance().draw();
        }
        GlStateManager.color(1, 1, 1, 1);
    }
    private record Region(ResourceLocation sheet, int u, int v) { }
    private static final class Icon extends GuiButton {
        private final Supplier<Region> region;
        private final Supplier<List<String>> tooltip;
        Icon(int id, int x, int y, int width, int height, Supplier<Region> region, Supplier<List<String>> tooltip) {
            super(id, x, y, width, height, ""); this.region = region; this.tooltip = tooltip;
        }
        @Override public void drawButton(Minecraft mc, int x, int y, float partial) {
            if (!visible) return;
            hovered = x >= this.x && y >= this.y && x < this.x + width && y < this.y + height;
            Region sprite = region.get(); drawTexture(mc, sprite.sheet, this.x, this.y, sprite.u, sprite.v, width, height, 1);
        }
    }
}
