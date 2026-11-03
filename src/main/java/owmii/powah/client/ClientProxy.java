package owmii.powah.client;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.*;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import owmii.powah.*;
import owmii.powah.block.*;
import owmii.powah.item.ChargedSnowball;

public final class ClientProxy extends CommonProxy {
    @Override public void preInit() {
        MinecraftForge.EVENT_BUS.register(this);
        ClientRegistry.bindTileEntitySpecialRenderer(OrbTile.class, new MachineRenderer.Orb());
        ClientRegistry.bindTileEntitySpecialRenderer(MachineTile.class, new MachineRenderer.Rod());
        ClientRegistry.bindTileEntitySpecialRenderer(ReactorTile.class, new MachineRenderer.Reactor());
        RenderingRegistry.registerEntityRenderingHandler(ChargedSnowball.class, manager -> new net.minecraft.client.renderer.entity.RenderSnowball<>(manager, Content.ITEMS.get("charged_snowball"), Minecraft.getMinecraft().getRenderItem()));
    }
    @Override public void init() {
        if (Loader.isModLoaded("ponder-legacy")) FMLInterModComms.sendMessage("ponder-legacy", "register_ponder_plugin", "owmii.powah.compat.ponder.PowahPonderPlugin");
    }
    @SubscribeEvent public void models(ModelRegistryEvent event) {
        Content.ITEMS.values().forEach(item -> ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory")));
        Content.BLOCKS.values().forEach(block -> {
            if (!(block instanceof MachineBlock machine)) return;
            StateMap.Builder state = new StateMap.Builder();
            if (!Set.of("energizing_rod", "energizing_orb", "ender_gate", "energy_hopper", "furnator", "magmator", "energy_discharger").contains(machine.kind)) state.ignore(MachineBlock.FACING);
            if (!machine.kind.equals("reactor")) state.ignore(MachineBlock.ASSEMBLED);
            ModelLoader.setCustomStateMapper(machine, state.build());
        });
    }
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id < 0 || id > 6 || !(world.getTileEntity(new BlockPos(x, y, z)) instanceof MachineTile tile)) return null;
        return tile.controller() == null ? null : new MachineScreen(player.inventory, tile.controller(), id == 0 ? EnumFacing.NORTH : EnumFacing.byIndex(id - 1));
    }
    @Override public void openBook() { Minecraft.getMinecraft().displayGuiScreen(new GuideScreen()); }
    @SubscribeEvent public void hud(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.world == null || mc.currentScreen != null || mc.objectMouseOver == null || mc.objectMouseOver.getBlockPos() == null) return;
        if (!(mc.world.getTileEntity(mc.objectMouseOver.getBlockPos()) instanceof MachineTile clicked)) return;
        MachineTile tile = clicked.controller(); if (tile == null) return;
        String text = tile instanceof OrbTile orb ? String.format("%,d / %,d FE", orb.progress, orb.required) : String.format("%,d / %,d FE", tile.energy().stored(), tile.energy().capacity());
        int x = (event.getResolution().getScaledWidth() - mc.fontRenderer.getStringWidth(text)) / 2;
        int y = event.getResolution().getScaledHeight() - 70;
        net.minecraft.client.gui.Gui.drawRect(x - 5, y - 4, x + mc.fontRenderer.getStringWidth(text) + 5, y + 13, 0xb51b2430);
        mc.fontRenderer.drawStringWithShadow(text, x, y, 0x72d6e0);
    }
}
