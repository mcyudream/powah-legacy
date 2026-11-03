package owmii.powah.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import owmii.powah.Powah;
import owmii.powah.block.*;
import org.lwjgl.opengl.GL11;

public final class MachineRenderer {
    public static final class Orb extends TileEntitySpecialRenderer<OrbTile> {
        @Override public void render(OrbTile tile, double x, double y, double z, float partial, int stage, float alpha) {
            int count = 0; for (int slot = 0; slot < 7; slot++) if (!tile.inventory.getStackInSlot(slot).isEmpty()) count++;
            if (count == 0) return;
            float angle = (tile.getWorld().getTotalWorldTime() + partial) * 2;
            int index = 0;
            for (int slot = 0; slot < 7; slot++) {
                ItemStack stack = tile.inventory.getStackInSlot(slot); if (stack.isEmpty()) continue;
                double rotation = Math.toRadians(angle + 360.0 * index++ / count);
                GlStateManager.pushMatrix();
                GlStateManager.translate(x + .5 + (count > 1 ? Math.cos(rotation) * .22 : 0), y + .55, z + .5 + (count > 1 ? Math.sin(rotation) * .22 : 0));
                GlStateManager.rotate(angle, 0, 1, 0); GlStateManager.scale(.32, .32, .32);
                RenderHelper.enableStandardItemLighting();
                Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.GROUND);
                GlStateManager.popMatrix();
            }
        }
    }
    public static final class Reactor extends TileEntitySpecialRenderer<ReactorTile> {
        private final ModelRenderer model;
        public Reactor() {
            ModelBase base = new ModelBase() { }; base.textureWidth = 256; base.textureHeight = 128;
            model = new ModelRenderer(base, 0, 0); model.mirror = true; model.addBox(-24, -32, -24, 48, 64, 48); model.setRotationPoint(0, -8, 0);
        }
        @Override public void render(ReactorTile tile, double x, double y, double z, float partial, int stage, float alpha) {
            if (!tile.assembled || tile.corePos != null) return;
            GlStateManager.pushMatrix(); GlStateManager.translate(x + .5, y + 1.5, z + .5); GlStateManager.scale(1, -1, -1);
            GlStateManager.enableBlend(); GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); GlStateManager.disableCull();
            layer("reactor"); if (tile.active) layer("reactor_on"); if (tile.fuel > 0) layer("reactor_filled");
            if (tile.tier() != Tier.STARTER) layer("reactor_" + tile.tier().id());
            GlStateManager.enableCull(); GlStateManager.disableBlend(); GlStateManager.popMatrix();
        }
        private void layer(String name) { bindTexture(Powah.id("textures/model/tile/" + name + ".png")); model.render(1f / 16); }
        @Override public boolean isGlobalRenderer(ReactorTile tile) { return tile.assembled && tile.corePos == null; }
    }
    public static final class Rod extends TileEntitySpecialRenderer<MachineTile> {
        @Override public void render(MachineTile tile, double x, double y, double z, float partial, int stage, float alpha) {
            if (!tile.kind().equals("energizing_rod") || !tile.active || tile.linkedOrb == null) return;
            var delta = tile.linkedOrb.subtract(tile.getPos());
            GlStateManager.pushMatrix(); GlStateManager.translate(x + .5, y + .5, z + .5);
            GlStateManager.disableTexture2D(); GlStateManager.disableLighting(); GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glLineWidth(2);
            Tessellator tess = Tessellator.getInstance(); BufferBuilder buffer = tess.getBuffer();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0, 0, 0).color(110, 225, 255, 220).endVertex(); buffer.pos(delta.getX(), delta.getY() + .1, delta.getZ()).color(185, 245, 255, 80).endVertex(); tess.draw();
            GL11.glLineWidth(1); GlStateManager.disableBlend(); GlStateManager.enableLighting(); GlStateManager.enableTexture2D(); GlStateManager.popMatrix();
        }
    }
}
