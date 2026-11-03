package owmii.powah.dev;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.resources.Language;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.*;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import owmii.powah.*;
import owmii.powah.block.*;
import owmii.powah.client.*;
import owmii.powah.energy.EnderNetwork;
import owmii.powah.item.*;
import owmii.powah.network.Controls;

/** Opt-in real-client/integrated-server verification; never included in the release jar. */
@Mod(modid = "powah_verification", name = "Powah verification", version = "1", dependencies = "required-after:powah-legacy", clientSideOnly = true, acceptableRemoteVersions = "*")
public final class PowahSmoke {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final JsonArray checks = new JsonArray();
    private CompletableFuture<Void> pending;
    private Path output;
    private String capture;
    private int phase, ticks, waitUntil, exitAt = Integer.MAX_VALUE;
    private long savedEnergy;
    private Language oldLanguage;
    private final String worldName = "powah-smoke-" + Long.toHexString(System.currentTimeMillis());
    private long cableStart;
    private int uiIndex, uiTankBefore;
    private static final BlockPos FURNACE = new BlockPos(0, 4, 0), MAGMA = new BlockPos(5, 4, 0), SOLAR = new BlockPos(10, 4, 0), THERMO = new BlockPos(15, 4, 0);
    private static final BlockPos ORB = new BlockPos(0, 4, 6), ROD = ORB.east(), SOURCE = new BlockPos(0, 4, 12), SINK = SOURCE.east(3);
    private static final BlockPos ENDER = new BlockPos(20, 4, 12), GATE = new BlockPos(25, 4, 12), WIRELESS = GATE.east();
    private static final BlockPos HOPPER = new BlockPos(10, 5, 12), DISCHARGER = new BlockPos(15, 4, 12), TRANSMITTER = new BlockPos(20, 4, 0), REACTOR = new BlockPos(-12, 4, 0);
    private static final BlockPos UI_CELL = new BlockPos(-6, 4, 12), UI_ENDER = new BlockPos(-10, 4, 12);
    private static final BlockPos[] UI_POSITIONS = {UI_CELL, UI_ENDER, GATE, FURNACE, MAGMA, THERMO, SOLAR, HOPPER, DISCHARGER, TRANSMITTER, SOURCE.east()};
    private static final String[] UI_NAMES = {"energy-cell", "ender-cell", "ender-gate", "furnator", "magmator", "thermo", "solar", "hopper", "discharger", "transmitter", "cable"};
    @Mod.EventHandler public void init(FMLInitializationEvent event) throws Exception {
        if (!Boolean.getBoolean("powah.dev.smoke")) return;
        output = mc.gameDir.toPath().resolve("powah-smoke"); Files.createDirectories(output.resolve("screenshots"));
        mc.gameSettings.pauseOnLostFocus = false; mc.gameSettings.renderDistanceChunks = 3; mc.gameSettings.guiScale = 2;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        MinecraftForge.EVENT_BUS.register(this); report("STARTING", null);
    }
    private void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
        synchronized (checks) { checks.add(message); } System.out.println("POWAH CHECK: " + message);
    }
    private void server(Runnable action) {
        pending = new CompletableFuture<>(); mc.getIntegratedServer().addScheduledTask(() -> {
            try { action.run(); pending.complete(null); } catch (Throwable failure) { pending.completeExceptionally(failure); }
        });
    }
    private boolean ready() { if (pending == null || !pending.isDone()) return false; pending.join(); return true; }
    private WorldServer world() { return mc.getIntegratedServer().getWorld(0); }
    private EntityPlayerMP player() { return mc.getIntegratedServer().getPlayerList().getPlayers().get(0); }
    private MachineTile tile(BlockPos pos) { return (MachineTile) world().getTileEntity(pos); }
    private MachineTile place(BlockPos pos, String id) { world().setBlockState(pos, Content.BLOCKS.get(id).getDefaultState(), 3); return tile(pos); }
    private void input(BlockPos pos, int slot, String id, int count) { ItemStack stack = Content.stack(id); stack.setCount(count); tile(pos).inventory.setStackInSlot(slot, stack); }
    private WorldSettings settings() {
        WorldSettings value = new WorldSettings(11225031, GameType.CREATIVE, false, false, WorldType.FLAT);
        value.setGeneratorOptions("3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;"); value.enableCommands(); return value;
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++; if (ticks >= exitAt) { mc.shutdown(); return; }
        if (exitAt != Integer.MAX_VALUE || ticks < waitUntil || capture != null) return;
        try {
            if (ticks > 3500) throw new IllegalStateException("Timeout in phase " + phase);
            switch (phase) {
                case 0:
                    if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                    check(Loader.isModLoaded("powah-legacy") && Loader.isModLoaded("cleanroom"), "Cleanroom discovers the packaged Powah mod");
                    String expectedCleanroom = System.getProperty("powah.expectedCleanroom");
                    if (expectedCleanroom != null) {
                        check(expectedCleanroom.equals(Loader.instance().getIndexedModList().get("cleanroom").getVersion()), "The client runs on official Cleanroom " + expectedCleanroom);
                        check(Loader.instance().getIndexedModList().get(Powah.LEGACY_MOD_ID).getSource().getName().endsWith(".jar"), "The client loads an installation jar");
                    }
                    check(Content.BLOCKS.size() == 107 && Content.ITEMS.size() == 145, "All 107 blocks and 38 standalone items are registered");
                    check(ForgeRegistries.RECIPES.getKeys().stream().filter(id -> id.getNamespace().equals(Powah.LEGACY_MOD_ID)).count() == 156, "All 156 crafting recipes load under the Legacy loader identity");
                    verifyModels(); mc.launchIntegratedServer(worldName, "Powah verification", settings()); phase++; break;
                case 1:
                    if (mc.world == null || mc.player == null || mc.getIntegratedServer().getPlayerList().getPlayers().isEmpty()) return;
                    mc.displayGuiScreen(null); server(this::setup); phase++; break;
                case 2:
                    if (!ready()) return;
                    mc.displayGuiScreen(new ModelsPreview(0)); capture = "models-machines.png"; phase++; waitUntil = ticks + 20; break;
                case 3:
                    server(() -> check(tile(SINK).energy().stored() > 0 && tile(SINK).energy().stored() <= 500L * (world().getTotalWorldTime() - cableStart + 1), "Mixed-tier route honors the starter cable bottleneck on real ticks")); phase = 30; waitUntil = ticks + 120; break;
                case 30:
                    if (!ready()) return;
                    server(this::verifyMachines); phase = 4; break;
                case 4:
                    if (!ready()) return;
                    mc.displayGuiScreen(new ModelsPreview(1)); capture = "models-materials.png"; phase++; break;
                case 5:
                    mc.displayGuiScreen(new GuideScreen()); capture = "guide-en.png"; phase = 51; break;
                case 51:
                    ((GuideScreen) mc.currentScreen).navigateTo("generators/furnator.md");
                    capture = "guide-furnator-en.png"; phase++; break;
                case 52:
                    keyScreen('\0', org.lwjgl.input.Keyboard.KEY_END);
                    check((int) net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(GuideScreen.class, (GuideScreen) mc.currentScreen, "scroll") > 0,
                        "GuideME-style reader scrolls to the original crafting recipes");
                    capture = "guide-recipes-en.png"; phase++; break;
                case 53:
                    clickControl(1); check(((GuideScreen) mc.currentScreen).getCurrentPageId().equals("index.md"), "Guide history returns to the home page");
                    clickControl(2); check(((GuideScreen) mc.currentScreen).getCurrentPageId().equals("generators/furnator.md"), "Guide history restores the next page");
                    clickControl(3); clickControl(3); clickControl(0);
                    for (char c : "Reactor".toCharArray()) keyScreen(c, 0);
                    capture = "guide-search-en.png"; phase++; break;
                case 54:
                    clickGuideLink("generators/reactor.md");
                    check(((GuideScreen) mc.currentScreen).getCurrentPageId().equals("generators/reactor.md"), "Full-text guide search opens the matching Reactor page");
                    capture = "guide-reactor-en.png"; phase = 6; break;
                case 6:
                    oldLanguage = mc.getLanguageManager().getCurrentLanguage(); mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguage("zh_cn")); mc.gameSettings.language = "zh_cn"; mc.refreshResources();
                    mc.displayGuiScreen(new GuideScreen()); capture = "guide-zh.png"; phase = 55; break;
                case 55:
                    ((GuideScreen) mc.currentScreen).navigateTo("generators/furnator.md"); capture = "guide-furnator-zh.png"; phase++; break;
                case 56:
                    mc.gameSettings.guiScale = 4; mc.displayGuiScreen(new GuideScreen());
                    ((GuideScreen) mc.currentScreen).navigateTo("generators/reactor.md"); capture = "guide-zh-large-scale.png"; phase++; break;
                case 57:
                    clickControl(0); for (char c : "反应堆".toCharArray()) keyScreen(c, 0);
                    capture = "guide-search-zh-large-scale.png"; phase++; break;
                case 58:
                    clickGuideLink("generators/reactor.md");
                    check(((GuideScreen) mc.currentScreen).getCurrentPageId().equals("generators/reactor.md"), "Chinese guide search remains usable at GUI scale 4");
                    phase = 7; break;
                case 7:
                    mc.player.closeScreen(); mc.gameSettings.guiScale = 4;
                    server(() -> player().setPositionAndUpdate(0, 4, 4));
                    phase = 48; waitUntil = ticks + 20; break;
                case 48:
                    if (!ready()) return;
                    server(() -> player().openGui(Powah.INSTANCE, 0, world(), FURNACE.getX(), FURNACE.getY(), FURNACE.getZ()));
                    phase = 8; waitUntil = ticks + 15; break;
                case 8:
                    if (!ready() || !(mc.currentScreen instanceof MachineScreen)) return;
                    capture = "machine-zh-large-scale.png"; clickControl(6); phase++; waitUntil = ticks + 15; break;
                case 9:
                    server(() -> { check(tile(FURNACE).redstoneMode == 1, "Authenticated machine GUI packet changes redstone mode on the server (mode="
                        + tile(FURNACE).redstoneMode + ", container=" + player().openContainer.getClass().getSimpleName() + ")"); savedEnergy = tile(FURNACE).energy().stored(); }); phase++; break;
                case 10:
                    if (!ready()) return;
                    mc.player.closeScreen(); mc.gameSettings.guiScale = 2; mc.getLanguageManager().setCurrentLanguage(oldLanguage); mc.gameSettings.language = oldLanguage.getLanguageCode(); mc.refreshResources();
                    phase++; waitUntil = ticks + 20; break;
                case 11:
                    if (!ready()) return;
                    server(() -> { player().setPositionAndUpdate(REACTOR.getX(), 4, 5); player().openGui(Powah.INSTANCE, 0, world(), REACTOR.getX(), REACTOR.getY(), REACTOR.getZ()); });
                    phase++; waitUntil = ticks + 15; break;
                case 12:
                    if (!ready() || !(mc.currentScreen instanceof MachineScreen)) return;
                    capture = "reactor-ui.png"; phase = 40; break;
                case 40:
                    if (uiIndex == UI_POSITIONS.length) { phase = 13; break; }
                    mc.player.closeScreen(); server(() -> {
                        if (uiIndex == 0) {
                            MachineTile cell = place(UI_CELL, "energy_cell_basic"); cell.energy().set(2_000_000);
                            cell.inventory.setStackInSlot(0, Content.stack("battery_starter")); cell.inventory.setStackInSlot(1, Content.stack("battery_starter"));
                            cell.update();
                            check(ItemEnergy.stored(cell.inventory.getStackInSlot(0)) == 1000 && ItemEnergy.stored(cell.inventory.getStackInSlot(1)) == 1000,
                                "Both original Energy Cell slots charge items and conserve energy");
                            check(cell.energy().stored() == 1_998_000, "Two-slot charging consumes exactly the transferred 2,000 FE");
                            cell.redstoneMode = 1; cell.sync();
                            MachineTile ender = place(UI_ENDER, "ender_cell_nitro"); ender.owner = player().getUniqueID(); ender.channel = 11; ender.sync();
                        }
                        BlockPos position = UI_POSITIONS[uiIndex]; player().setPositionAndUpdate(position.getX(), position.getY(), position.getZ() + 2);
                    }); phase = 47; waitUntil = ticks + 12; break;
                case 47:
                    if (!ready() || !(mc.world.getTileEntity(UI_POSITIONS[uiIndex]) instanceof MachineTile)) return;
                    server(() -> {
                        BlockPos position = UI_POSITIONS[uiIndex];
                        var state = world().getBlockState(position);
                        ((MachineBlock) state.getBlock()).onBlockActivated(world(), position, state, player(), EnumHand.MAIN_HAND, EnumFacing.EAST, 1, .5f, .5f);
                    }); phase = 41; waitUntil = ticks + 12; break;
                case 41:
                    if (!ready() || !(mc.currentScreen instanceof MachineScreen)) return;
                    capture = "ui-" + UI_NAMES[uiIndex] + "-en.png";
                    if (uiIndex == 1) clickControl(Controls.CHANNEL_BASE + 10);
                    if (uiIndex == 2) clickControl(Controls.ALL_SIDES);
                    if (uiIndex == UI_POSITIONS.length - 1) clickControl(EnumFacing.EAST.ordinal());
                    if (uiIndex == 4) server(() -> { uiTankBefore = tile(MAGMA).tank.getFluidAmount(); player().inventory.setItemStack(new ItemStack(Items.LAVA_BUCKET));
                        player().connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, player().inventory.getItemStack())); });
                    phase = 42; waitUntil = ticks + 12; break;
                case 42:
                    if (!ready()) return;
                    if (uiIndex == 4) {
                        clickTank(0); phase = 43; waitUntil = ticks + 12; break;
                    }
                    server(() -> {
                        if (uiIndex == 1) {
                            check(tile(UI_ENDER).channel == 10, "Original Ender channel button selects channel 11 directly on the server");
                            player().inventory.setInventorySlotContents(10, Content.stack("battery_starter"));
                            player().openContainer.detectAndSendChanges();
                        }
                        if (uiIndex == 2) check(java.util.Arrays.equals(tile(GATE).sides, new byte[]{1, 1, 1, 1, 1, 1}), "Original all-sides widget configures every face");
                        if (uiIndex == UI_POSITIONS.length - 1) {
                            var container = (owmii.powah.inventory.MachineContainer) player().openContainer;
                            check(container.cableSide == EnumFacing.EAST && tile(SOURCE.east()).sides[5] == 1 && tile(SOURCE.east()).sides[2] == 0,
                                "Cable UI configures the face actually clicked in the world");
                            check(container.inventorySlots.isEmpty(), "Original compact cable UI has no player inventory slots");
                        }
                    }); phase = uiIndex == 1 ? 49 : 44; waitUntil = ticks + 12; break;
                case 49:
                    if (!ready()) return;
                    var container = mc.player.openContainer;
                    var sourceSlot = container.inventorySlots.stream().filter(slot -> slot.inventory == mc.player.inventory && slot.getSlotIndex() == 10).findFirst().orElseThrow();
                    mc.playerController.windowClick(container.windowId, sourceSlot.slotNumber, 0, ClickType.QUICK_MOVE, mc.player);
                    phase = 50; waitUntil = ticks + 20; break;
                case 50:
                    server(() -> check(tile(UI_ENDER).energy().capacity() == 1_000_000 && tile(UI_ENDER).inventory.getStackInSlot(0).isEmpty()
                        && player().inventory.getStackInSlot(10).isEmpty(), "Shift-clicking a battery through the original Ender UI consumes it and expands the selected channel"));
                    phase = 44; break;
                case 43:
                    server(() -> {
                        check(player().inventory.getItemStack().getItem() == Items.BUCKET && tile(MAGMA).tank.getFluidAmount() == uiTankBefore + 1000,
                            "Original tank click inserts a lava bucket and returns its empty container");
                    }); phase = 45; break;
                case 45:
                    if (!ready()) return;
                    clickTank(1); phase = 46; waitUntil = ticks + 12; break;
                case 46:
                    server(() -> {
                        check(player().inventory.getItemStack().getItem() == Items.LAVA_BUCKET && tile(MAGMA).tank.getFluidAmount() == uiTankBefore,
                            "Original tank right-click extracts exactly one lava bucket (cursor=" + player().inventory.getItemStack()
                                + ", tank=" + tile(MAGMA).tank.getFluidAmount() + ", before=" + uiTankBefore + ")");
                        player().inventory.setItemStack(ItemStack.EMPTY);
                        player().connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, ItemStack.EMPTY));
                    }); phase = 44; break;
                case 44:
                    if (!ready()) return;
                    uiIndex++; phase = 40; break;
                case 13:
                    mc.player.closeScreen(); server(() -> player().setPositionAndUpdate(REACTOR.getX() + 5, 5, 8));
                    phase = 32; waitUntil = ticks + 15; break;
                case 32:
                    if (!ready()) return;
                    mc.player.rotationYaw = mc.player.prevRotationYaw = 148;
                    mc.player.rotationPitch = mc.player.prevRotationPitch = 4; capture = "reactor-world.png";
                    phase = 31; break;
                case 31:
                    server(() -> { check(tile(FURNACE).energy().stored() == savedEnergy, "Redstone-disabled Furnator preserves energy and fuel over real ticks"); verifyReactorDismantle(); mc.getIntegratedServer().saveAllWorlds(false); mc.getIntegratedServer().getPlayerList().saveAllPlayerData(); }); phase = 14; break;
                case 14:
                    if (!ready()) return;
                    // loadWorld(null) shuts down and joins the integrated server.
                    // A separate disconnect can stop it before initiateShutdown's
                    // queued task runs, leaving the client waiting on a dead thread.
                    mc.loadWorld(null); mc.displayGuiScreen(new GuiMainMenu()); phase++; waitUntil = ticks + 40; break;
                case 15:
                    if (mc.getIntegratedServer() != null) return;
                    mc.launchIntegratedServer(worldName, "Powah verification", settings()); phase++; break;
                case 16:
                    if (mc.world == null || mc.player == null || mc.getIntegratedServer().getPlayerList().getPlayers().isEmpty()) return;
                    server(() -> {
                        check(tile(FURNACE).energy().stored() == savedEnergy && tile(FURNACE).redstoneMode == 1, "Machine energy and controls survive a complete world close/reload");
                        check(EnderNetwork.get(world()).channel(player().getUniqueID(), 0).capacity() == 4_000_000, "Ender channel capacity survives world close/reload");
                        check(EnderNetwork.get(world()).channel(player().getUniqueID(), 1).capacity() == 1_000_000
                                && EnderNetwork.get(world()).channel(player().getUniqueID(), 1).stored() == 0,
                            "An empty Ender capacity extension survives world close/reload");
                        check(tile(WIRELESS).energy().stored() == 12345, "Wireless transferred energy survives world close/reload");
                        check(((ReactorTile) tile(REACTOR)).assembled, "Reactor multiblock and controller links survive world close/reload");
                    }); phase++; break;
                case 17:
                    if (!ready()) return;
                    mc.displayGuiScreen(null);
                    if (Loader.isModLoaded("ponder-legacy")) {
                        Class.forName("owmii.powah.dev.PonderVerification").getMethod("verify").invoke(null);
                        checks.add("All Powah Ponder registrations have no diagnostics; five tutorials play through their timelines");
                        Class.forName("owmii.powah.dev.PonderVerification").getMethod("open").invoke(null); capture = "ponder.png"; phase++; waitUntil = ticks + 65;
                    } else {
                        check(!Loader.isModLoaded("ponder-legacy"), "Standalone client, guide, machines and world reload work with Ponder absent"); phase = 19;
                    }
                    break;
                case 18:
                    capture = "ponder-playing.png"; phase = 33; break;
                case 33:
                    mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguage("zh_cn")); mc.gameSettings.language = "zh_cn"; mc.refreshResources();
                    Class.forName("owmii.powah.dev.PonderVerification").getMethod("open").invoke(null);
                    phase++; waitUntil = ticks + 65; break;
                case 34:
                    capture = "ponder-zh.png"; phase++; break;
                case 35:
                    mc.getLanguageManager().setCurrentLanguage(oldLanguage); mc.gameSettings.language = oldLanguage.getLanguageCode(); mc.refreshResources();
                    Class.forName("owmii.powah.dev.PonderVerification").getMethod("openReactor").invoke(null);
                    phase++; waitUntil = ticks + 110; break;
                case 36:
                    capture = "ponder-reactor.png"; phase = 19; break;
                case 19:
                    report("PASS", null); exitAt = ticks + 10; break;
            }
        } catch (Throwable failure) { failure.printStackTrace(); report("FAIL", failure); exitAt = ticks + 10; }
    }
    private void clickControl(int id) throws Exception {
        List<GuiButton> buttons = net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(GuiScreen.class, mc.currentScreen, "buttonList", "field_146292_n");
        GuiButton button = buttons.stream().filter(candidate -> candidate.id == id).findFirst().orElseThrow();
        check(button.x >= 0 && button.y >= 0 && button.x + button.getButtonWidth() <= mc.currentScreen.width,
            "Original control " + id + " remains visible at GUI scale " + mc.gameSettings.guiScale);
        clickScreen(button.x + 2, button.y + 2, 0);
    }
    private void clickTank(int button) throws Exception {
        var screen = (net.minecraft.client.gui.inventory.GuiContainer) mc.currentScreen;
        clickScreen(screen.getGuiLeft() + 160, screen.getGuiTop() + 10, button);
    }
    private void clickScreen(int x, int y, int button) throws Exception {
        var click = net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(mc.currentScreen.getClass(), "mouseClicked", "func_73864_a", int.class, int.class, int.class);
        click.setAccessible(true); click.invoke(mc.currentScreen, x, y, button);
    }
    private void keyScreen(char character, int key) throws Exception {
        var input = net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(GuideScreen.class, "keyTyped", "func_73869_a", char.class, int.class);
        input.setAccessible(true); input.invoke(mc.currentScreen, character, key);
    }
    private void clickGuideLink(String target) throws Exception {
        List<?> parts = net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(GuideScreen.class, (GuideScreen) mc.currentScreen, "document");
        int left = net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(GuideScreen.class, (GuideScreen) mc.currentScreen, "documentLeft");
        for (Object part : parts) {
            var targetField = part.getClass().getDeclaredField("target"); targetField.setAccessible(true);
            if (!target.equals(targetField.get(part))) continue;
            var xField = part.getClass().getDeclaredField("x"); xField.setAccessible(true);
            var yField = part.getClass().getDeclaredField("y"); yField.setAccessible(true);
            clickScreen(left + xField.getInt(part) + 2, 25 + yField.getInt(part) + 2, 0); return;
        }
        throw new IllegalStateException("No guide link for " + target);
    }
    private void verifyModels() {
        var missing = mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel();
        for (var entry : Content.ITEMS.entrySet()) {
            ItemStack stack = new ItemStack(entry.getValue());
            if (stack.getDisplayName().endsWith(".name")) throw new IllegalStateException("Broken legacy item translation: " + entry.getKey());
            IBakedModel model = mc.getRenderItem().getItemModelWithOverrides(stack, null, null);
            check(model != missing && !model.getParticleTexture().getIconName().equals("minecraft:missingno"), "Item model and texture: " + entry.getKey());
        }
        for (var entry : Content.BLOCKS.entrySet()) {
            var block = entry.getValue();
            for (var state : block.getBlockState().getValidStates()) {
                IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(state);
                boolean hiddenReactor = block instanceof MachineBlock machine && machine.kind.equals("reactor") && state.getValue(MachineBlock.ASSEMBLED);
                if (model == missing || !hiddenReactor && model.getParticleTexture().getIconName().equals("minecraft:missingno"))
                    throw new IllegalStateException("Missing world model or texture " + entry.getKey() + " " + state);
            }
        }
        check(true, "Every registered block state has a baked world model");
    }
    private void setup() {
        world().setWorldTime(6000); world().getGameRules().setOrCreateGameRule("doDaylightCycle", "false"); world().getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        world().getWorldInfo().setDifficulty(EnumDifficulty.PEACEFUL);
        // A fresh flat world may spawn slimes before the player has joined and
        // this fixture can set its gamerules. Clear the isolated test area.
        for (var entity : world().getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(REACTOR).grow(5)))
            if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) entity.setDead();
        for (BlockPos pos : BlockPos.getAllInBox(REACTOR.add(-1, 0, -1), REACTOR.add(1, 3, 1))) world().getChunk(pos);
        world().getWorldInfo().setRaining(false); player().setPositionAndUpdate(0, 4, 4);
        place(FURNACE, "furnator_basic"); tile(FURNACE).inventory.setStackInSlot(1, new ItemStack(Items.COAL));
        place(MAGMA, "magmator_basic"); tile(MAGMA).tank.fill(new FluidStack(FluidRegistry.LAVA, 500), true);
        place(SOLAR, "solar_panel_basic");
        world().setBlockState(THERMO.down(), Blocks.MAGMA.getDefaultState(), 3); place(THERMO, "thermo_generator_basic"); tile(THERMO).tank.fill(new FluidStack(FluidRegistry.WATER, 1000), true);
        OrbTile orb = (OrbTile) place(ORB, "energizing_orb");
        orb.inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT)); orb.inventory.setStackInSlot(2, new ItemStack(Items.GOLD_INGOT));
        check(orb.fill(1000) == 1000, "Orb accepts recipe-specific energy");
        orb.inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND));
        check(orb.fill(1000) == 0 && orb.progress == 0 && orb.inventory.getStackInSlot(2).getItem() == Items.GOLD_INGOT, "Changing orb ingredients resets progress without consuming other inputs");
        orb.inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
        var automatedOrb = orb.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
        check(automatedOrb.extractItem(1, 1, true).isEmpty() && automatedOrb.extractItem(1, 1, false).isEmpty()
                && orb.inventory.getStackInSlot(1).getItem() == Items.IRON_INGOT, "Orb automation keeps recipe ingredients in place");
        place(ROD, "energizing_rod_basic").energy().set(10000);
        check(!orb.hasCapability(CapabilityEnergy.ENERGY, null), "Orb refuses ordinary Forge Energy input; rods provide its recipe energy");
        place(SOURCE, "energy_cell_basic").energy().set(40000);
        place(SOURCE.east(), "energy_cable_nitro"); place(SOURCE.east(2), "energy_cable_starter"); place(SINK, "energizing_rod_nitro");
        world().setBlockState(SINK, world().getBlockState(SINK).withProperty(MachineBlock.FACING, EnumFacing.WEST), 3); cableStart = world().getTotalWorldTime();
        // This rod's orb is farther than the configured range.
        MachineTile ender = place(ENDER, "ender_cell_basic"); ender.owner = player().getUniqueID();
        ItemStack extender = Content.stack("energy_cell_basic"); ItemEnergy.set(extender, 12345); ender.inventory.setStackInSlot(0, extender);
        MachineTile gate = place(GATE, "ender_gate_basic"); gate.owner = ender.owner;
        world().setBlockState(GATE, world().getBlockState(GATE).withProperty(MachineBlock.FACING, EnumFacing.EAST), 3); place(WIRELESS, "energy_hopper_basic");
        place(HOPPER, "energy_hopper_basic").energy().set(20000); world().setBlockState(HOPPER, world().getBlockState(HOPPER).withProperty(MachineBlock.FACING, EnumFacing.DOWN), 3);
        world().setBlockState(HOPPER.down(), Blocks.CHEST.getDefaultState(), 3); ((TileEntityChest) world().getTileEntity(HOPPER.down())).setInventorySlotContents(0, Content.stack("battery_starter"));
        place(DISCHARGER, "energy_discharger_basic"); ItemStack battery = Content.stack("battery_starter"); ItemEnergy.set(battery, 25000); tile(DISCHARGER).inventory.setStackInSlot(0, battery);
        place(TRANSMITTER, "player_transmitter_basic").energy().set(10000);
        ItemStack card = Content.stack("binding_card"); card.setTagCompound(new NBTTagCompound()); card.getTagCompound().setUniqueId("BoundPlayer", player().getUniqueID());
        tile(TRANSMITTER).inventory.setStackInSlot(0, card); player().inventory.setInventorySlotContents(9, Content.stack("battery_starter"));
        player().setGameType(GameType.SURVIVAL); player().setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Content.ITEMS.get("reactor_basic"), 36));
        EnumActionResult result = ((MachineItem) Content.ITEMS.get("reactor_basic")).onItemUse(player(), world(), REACTOR.down(), EnumHand.MAIN_HAND, EnumFacing.UP, .5f, 1, .5f);
        if (result != EnumActionResult.SUCCESS) throw new IllegalStateException("Reactor fixture placement " + result
            + "; held=" + player().getHeldItemMainhand() + "; entities=" + world().getEntitiesWithinAABBExcludingEntity(null,
                new AxisAlignedBB(REACTOR.add(-1, 0, -1), REACTOR.add(2, 4, 2))));
        check(result == EnumActionResult.SUCCESS && player().getHeldItemMainhand().isEmpty(), "Survival placement consumes exactly 36 parts and creates the complete reactor");
        check(world().getTileEntity(REACTOR.add(1, 3, 1)) instanceof ReactorTile, "All 36 reactor positions contain their linked tiles");
        input(REACTOR, 1, "uraninite", 10); tile(REACTOR).inventory.setStackInSlot(2, new ItemStack(Items.COAL, 8)); tile(REACTOR).inventory.setStackInSlot(3, new ItemStack(Items.REDSTONE, 8)); input(REACTOR, 4, "dry_ice", 8); tile(REACTOR).tank.fill(new FluidStack(FluidRegistry.WATER, 1000), true);
        player().setGameType(GameType.CREATIVE); verifyCrafting();
    }
    private void verifyCrafting() {
        InventoryCrafting grid = new InventoryCrafting(new Container() { @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return true; } }, 3, 3);
        for (int i = 0; i < 3; i++) grid.setInventorySlotContents(i, new ItemStack(Items.COAL));
        grid.setInventorySlotContents(3, new ItemStack(Items.CLAY_BALL)); grid.setInventorySlotContents(4, new ItemStack(Items.CLAY_BALL)); grid.setInventorySlotContents(5, new ItemStack(Items.LAVA_BUCKET));
        ItemStack result = CraftingManager.findMatchingResult(grid, world());
        check(result.getItem() == Content.ITEMS.get("dielectric_paste") && result.getCount() == 24, "Real crafting registry produces 24 Dielectric Paste");
        check(CraftingManager.findMatchingRecipe(grid, world()).getRemainingItems(grid).get(5).getItem() == Items.BUCKET, "Dielectric Paste recipe returns its empty bucket");
    }
    private void verifyMachines() {
        check(tile(FURNACE).energy().stored() > 0 && tile(FURNACE).inventory.getStackInSlot(1).isEmpty(), "Furnator consumes coal and produces energy on server ticks");
        check(tile(FURNACE).energy().stored() + tile(FURNACE).fuelBuffer == 48000, "Furnator conserves exactly 48,000 FE per coal");
        check(tile(MAGMA).energy().stored() + tile(MAGMA).fuelBuffer + tile(MAGMA).tank.getFluidAmount() * 100L == 50000, "Magmator conserves the supplied lava's 50,000 FE");
        check(tile(SOLAR).energy().stored() > 0, "Solar Panel generates in daylight with open sky");
        check(tile(THERMO).energy().stored() > 0 && tile(THERMO).tank.getFluidAmount() < 1000, "Thermo Generator uses its real heat source and water");
        check(tile(ORB).inventory.getStackInSlot(0).getItem() == Content.ITEMS.get("steel_energized") && tile(ORB).inventory.getStackInSlot(0).getCount() == 2, "Real rods charge an orb to produce exactly two Energized Steel");
        check(tile(ROD).energy().stored() == 0, "Energized Steel consumes exactly 10,000 FE from the rod");
        long cableTotal = tile(SOURCE).energy().stored() + tile(SOURCE.east()).energy().stored() + tile(SOURCE.east(2)).energy().stored() + tile(SINK).energy().stored();
        check(cableTotal == 40000 && tile(SINK).energy().stored() > 0, "Mixed-tier cable route moves energy and conserves all 40,000 FE");
        check(tile(SINK).energy().stored() <= 500L * 145, "Starter cable bottleneck limits the mixed-tier network");
        check(EnderNetwork.get(world()).channel(player().getUniqueID(), 0).capacity() == 4_000_000 && tile(ENDER).inventory.getStackInSlot(0).isEmpty(), "Ender Cell consumes one cell and adds exactly its capacity");
        check(tile(WIRELESS).energy().stored() == 12345, "Ender Gate transfers the extender's exact stored energy wirelessly");
        check(EnderNetwork.get(world()).channel(UUID.randomUUID(), 0).capacity() == 0 && EnderNetwork.get(world()).channel(player().getUniqueID(), 1).capacity() == 0, "Ender owners and channels stay isolated");
        MachineTile emptyExtender = place(ENDER.north(2), "ender_cell_basic"); emptyExtender.owner = player().getUniqueID(); emptyExtender.channel = 1;
        emptyExtender.inventory.setStackInSlot(0, Content.stack("battery_starter")); emptyExtender.update();
        check(EnderNetwork.get(world()).channel(player().getUniqueID(), 1).capacity() == 1_000_000
                && emptyExtender.inventory.getStackInSlot(0).isEmpty(), "An empty Battery still extends an Ender channel by its full capacity");
        check(ItemEnergy.stored(((TileEntityChest) world().getTileEntity(HOPPER.down())).getStackInSlot(0)) == 20000 && tile(HOPPER).energy().stored() == 0, "Energy Hopper transfers exactly 20,000 FE into a battery in a real chest");
        check(tile(DISCHARGER).energy().stored() == 25000 && ItemEnergy.stored(tile(DISCHARGER).inventory.getStackInSlot(0)) == 0, "Discharger extracts exactly the battery's 25,000 FE");
        check(ItemEnergy.stored(player().inventory.getStackInSlot(9)) == 10000 && tile(TRANSMITTER).energy().stored() == 0, "Bound Player Transmitter transfers exactly 10,000 FE into the player's inventory");
        check(((ReactorTile) tile(REACTOR)).fuel > 0 && tile(REACTOR).energy().stored() > 0, "Complete reactor consumes Uraninite and produces energy");
        check(tile(REACTOR.east()).getCapability(CapabilityEnergy.ENERGY, EnumFacing.EAST) != null, "Reactor side port delegates its energy capability to the controller");
        check(!tile(REACTOR.east()).hasCapability(CapabilityEnergy.ENERGY, EnumFacing.NORTH), "Reactor side port exposes energy only on its outer face");
        tile(REACTOR).sides[EnumFacing.EAST.ordinal()] = 3;
        check(!tile(REACTOR.east()).getCapability(CapabilityEnergy.ENERGY, EnumFacing.EAST).canExtract(), "Reactor side port honors the controller's side configuration");
        tile(REACTOR).sides[EnumFacing.EAST.ordinal()] = 0;
        OrbTile orb = (OrbTile) tile(ORB); orb.inventory.setStackInSlot(0, ItemStack.EMPTY);
        for (int i = 1; i <= 4; i++) orb.inventory.setStackInSlot(i, new ItemStack(Items.BLAZE_POWDER));
        check(orb.fill(119999) == 119999 && orb.inventory.getStackInSlot(0).isEmpty(), "Repeated-input recipe waits for its final FE");
        check(orb.fill(1) == 1 && orb.inventory.getStackInSlot(0).getItem() == Content.ITEMS.get("crystal_blazing"), "Four distinct blaze powder inputs produce one crystal at exactly 120,000 FE");
        var automatedOrb = orb.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
        check(automatedOrb.extractItem(0, 1, true).getItem() == Content.ITEMS.get("crystal_blazing")
                && !orb.inventory.getStackInSlot(0).isEmpty()
                && automatedOrb.extractItem(0, 1, false).getItem() == Content.ITEMS.get("crystal_blazing")
                && orb.inventory.getStackInSlot(0).isEmpty(), "Orb automation simulates and extracts only the completed result");
        tile(SOLAR).redstoneMode = 1; tile(THERMO).redstoneMode = 1; tile(MAGMA).redstoneMode = 1;
    }
    private void verifyReactorDismantle() {
        long energy = tile(REACTOR).energy().stored(); world().destroyBlock(REACTOR.add(1, 2, 1), true);
        List<EntityItem> drops = world().getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(REACTOR).grow(5));
        int count = drops.stream().filter(entity -> entity.getItem().getItem() == Content.ITEMS.get("reactor_basic")).mapToInt(entity -> entity.getItem().getCount()).sum();
        check(count == 36 && world().isAirBlock(REACTOR), "Breaking one reactor part dismantles all parts and refunds exactly 36");
        EntityItem part = drops.stream().filter(entity -> entity.getItem().getItem() == Content.ITEMS.get("reactor_basic")).findFirst().orElseThrow();
        check(part.getItem().getTagCompound().getCompoundTag("PowahData").getLong("Energy") == energy, "Dismantled reactor preserves its stored energy");
        player().setGameType(GameType.SURVIVAL); player().setHeldItem(EnumHand.MAIN_HAND, part.getItem().copy()); part.setDead();
        EnumActionResult placed = ((MachineItem) Content.ITEMS.get("reactor_basic")).onItemUse(player(), world(), REACTOR.down(), EnumHand.MAIN_HAND, EnumFacing.UP, .5f, 1, .5f);
        check(placed == EnumActionResult.SUCCESS && tile(REACTOR).energy().stored() == energy, "Reassembly restores reactor energy without duplication");
        player().setGameType(GameType.CREATIVE);
    }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || capture == null) return;
        try {
            var screenshot = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            javax.imageio.ImageIO.write(screenshot, "png", output.resolve("screenshots").resolve(capture).toFile()); capture = null;
        } catch (Exception failure) { report("FAIL", failure); exitAt = ticks + 10; capture = null; }
    }
    private void report(String result, Throwable error) {
        try {
            JsonObject report = new JsonObject(); report.addProperty("result", result); report.addProperty("withPonder", Loader.isModLoaded("ponder-legacy")); report.add("checks", checks);
            report.addProperty("cleanroom", Loader.instance().getIndexedModList().get("cleanroom").getVersion());
            if (error != null) report.addProperty("error", error.toString());
            Files.writeString(output.resolve("report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
}
