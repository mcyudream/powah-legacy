package owmii.powah.compat.ponder;

import java.util.*;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.*;
import net.createmod.ponder.api.scene.*;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import owmii.powah.*;
import owmii.powah.block.*;

/** Loaded by Ponder's IMC handler only when that separate mod is installed. */
public final class PowahPonderPlugin implements PonderPlugin {
    private static final ResourceLocation TAG = Powah.id("energy");
    @Override public String getModId() { return Powah.MOD_ID; }
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        Content.BLOCKS.forEach((name, block) -> {
            if (!(block instanceof MachineBlock machine)) return;
            String scene = switch (machine.kind) {
                case "energizing_rod", "energizing_orb" -> "energizing";
                case "reactor" -> "reactor";
                case "ender_cell", "ender_gate" -> "ender";
                case "energy_hopper", "energy_discharger", "player_transmitter" -> "charging";
                default -> "power";
            };
            register(helper, name, scene);
        });
        register(helper, "book", "power");
        for (String name : Content.ITEMS.keySet()) if (name.startsWith("battery_")) register(helper, name, "charging");
        for (String name : List.of("steel_energized", "crystal_blazing", "crystal_niotic", "crystal_spirited", "crystal_nitro", "ender_core")) register(helper, name, "energizing");
    }
    private void register(PonderSceneRegistrationHelper<ResourceLocation> helper, String item, String scene) {
        String sceneId = scene + "_" + item;
        PonderStoryBoard story = switch (scene) {
            case "energizing" -> (builder, util) -> energizing(builder, util, sceneId);
            case "reactor" -> (builder, util) -> reactor(builder, util, sceneId);
            case "ender" -> (builder, util) -> ender(builder, util, sceneId);
            case "charging" -> (builder, util) -> charging(builder, util, sceneId);
            default -> (builder, util) -> power(builder, util, sceneId);
        };
        helper.addStoryBoard(Powah.id(item), Powah.id(scene), story, TAG).identifiedBy(Powah.id(sceneId));
    }
    @Override public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(TAG).title("Powah! Energy").description("Generate, store, transmit and use Forge Energy.")
            .item(Content.ITEMS.get("energy_cell_basic")).addToIndex().register();
        for (String name : Content.BLOCKS.keySet()) helper.addToTag(TAG).add(Powah.id(name));
    }
    @Override public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("power.1", "Feed furnace fuel into a Furnator to generate Forge Energy.");
        helper.registerSharedText("power.2", "Connect a generator to an Energy Cell using Energy Cables. The slowest cable limits the route.");
        helper.registerSharedText("power.3", "Solar Panels need daylight; Thermo Generators need water and a heat source underneath. Magmators consume lava.");
        helper.registerSharedText("energizing.1", "Insert one item at a time into the Energizing Orb. Iron and gold make two Energized Steel.");
        helper.registerSharedText("energizing.2", "Power an Energizing Rod through its attached face. It automatically finds an orb within four blocks.");
        helper.registerSharedText("energizing.3", "Several rods can charge one recipe. Extract the output before inserting the next recipe.");
        helper.registerSharedText("reactor.1", "Hold 36 matching Reactor parts and place them in a clear 3 x 4 x 3 space. The structure assembles automatically.");
        helper.registerSharedText("reactor.2", "Insert Uraninite. Coal and redstone improve efficiency; water and solid coolant reduce temperature.");
        helper.registerSharedText("reactor.3", "Export power at the bottom, lower side centers or top center. Removing any part dismantles the reactor and refunds all 36 parts.");
        helper.registerSharedText("ender.1", "Ender Cells and Gates share energy across dimensions when owner and channel match.");
        helper.registerSharedText("ender.2", "Open an Ender Cell and Shift-click an Energy Cell or Battery in your inventory. It is consumed to add its capacity and stored energy to the channel.");
        helper.registerSharedText("ender.3", "Configure the same channel on an Ender Gate. Its attached face can provide power without cables.");
        helper.registerSharedText("charging.1", "Bind a Binding Card by using it in the air, then put it into a powered Player Transmitter.");
        helper.registerSharedText("charging.2", "The transmitter charges that player's inventory in the same dimension. Upgrade the card using an Enderman for other dimensions.");
        helper.registerSharedText("charging.3", "An Energy Hopper charges batteries in the inventory it faces. An Energy Discharger extracts their power. Sneak-use a Battery to enable portable charging.");
    }
    private static void start(SceneBuilder scene, String id, String title) {
        scene.title(id, title); scene.configureBasePlate(0, 0, 7); scene.showBasePlate(); scene.idle(10);
    }
    private static void text(SceneBuilder scene, SceneBuildingUtil util, String key, int x, int y, int z) {
        scene.overlay().showText(85).sharedText(key).pointAt(util.vector().centerOf(x, y, z)).placeNearTarget().attachKeyFrame(); scene.idle(90);
    }
    private static void power(SceneBuilder scene, SceneBuildingUtil util, String id) {
        start(scene, id, "Generating and Storing Energy");
        scene.world().showSection(util.select().position(1, 1, 3), EnumFacing.DOWN);
        scene.overlay().showControls(util.vector().topOf(1, 1, 3), Pointing.DOWN, 40).rightClick().withItem(new ItemStack(Items.COAL));
        text(scene, util, "power.1", 1, 1, 3);
        scene.world().showSection(util.select().fromTo(2, 1, 3, 5, 1, 3), EnumFacing.DOWN);
        scene.overlay().showLine(PonderPalette.OUTPUT, util.vector().centerOf(1, 1, 3), util.vector().centerOf(5, 1, 3), 80);
        text(scene, util, "power.2", 3, 1, 3);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 5, 1, 1), EnumFacing.DOWN);
        text(scene, util, "power.3", 3, 1, 1); scene.markAsFinished();
    }
    private static void energizing(SceneBuilder scene, SceneBuildingUtil util, String id) {
        start(scene, id, "Energizing Materials"); scene.world().showSection(util.select().position(3, 1, 3), EnumFacing.DOWN);
        scene.overlay().showControls(util.vector().topOf(3, 1, 3), Pointing.DOWN, 45).rightClick().withItem(new ItemStack(Items.IRON_INGOT));
        text(scene, util, "energizing.1", 3, 1, 3);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 5, 1, 2), EnumFacing.DOWN);
        text(scene, util, "energizing.2", 2, 1, 2);
        scene.overlay().showLine(PonderPalette.OUTPUT, util.vector().centerOf(2, 1, 2), util.vector().centerOf(3, 1, 3), 85);
        scene.world().createItemEntity(util.vector().topOf(3, 1, 3), util.vector().of(0, .08, 0), Content.stack("steel_energized"));
        text(scene, util, "energizing.3", 3, 1, 3); scene.markAsFinished();
    }
    private static void reactor(SceneBuilder scene, SceneBuildingUtil util, String id) {
        start(scene, id, "Building a Reactor");
        for (int y = 1; y <= 4; y++) { scene.world().showSection(util.select().fromTo(2, y, 2, 4, y, 4), EnumFacing.DOWN); scene.idle(12); }
        scene.world().modifyBlocks(util.select().fromTo(2, 1, 2, 4, 4, 4), state -> state.withProperty(MachineBlock.ASSEMBLED, true), false);
        scene.world().modifyBlockEntity(util.grid().at(3, 1, 3), ReactorTile.class, tile -> tile.assembled = true);
        text(scene, util, "reactor.1", 3, 2, 3);
        scene.overlay().showControls(util.vector().centerOf(3, 2, 2), Pointing.RIGHT, 45).rightClick().withItem(Content.stack("uraninite"));
        text(scene, util, "reactor.2", 3, 2, 3);
        scene.world().showSection(util.select().fromTo(5, 1, 3, 6, 1, 3), EnumFacing.DOWN);
        text(scene, util, "reactor.3", 4, 1, 3); scene.markAsFinished();
    }
    private static void ender(SceneBuilder scene, SceneBuildingUtil util, String id) {
        start(scene, id, "Wireless Ender Networks"); scene.world().showSection(util.select().layersFrom(1), EnumFacing.DOWN);
        text(scene, util, "ender.1", 2, 1, 3);
        scene.overlay().showControls(util.vector().topOf(2, 1, 3), Pointing.DOWN, 50).rightClick().withItem(Content.stack("energy_cell_basic"));
        text(scene, util, "ender.2", 2, 1, 3);
        scene.overlay().showLine(PonderPalette.OUTPUT, util.vector().centerOf(2, 1, 3), util.vector().centerOf(5, 1, 3), 80);
        text(scene, util, "ender.3", 5, 1, 3); scene.markAsFinished();
    }
    private static void charging(SceneBuilder scene, SceneBuildingUtil util, String id) {
        start(scene, id, "Charging Items and Players"); scene.world().showSection(util.select().layersFrom(1), EnumFacing.DOWN);
        scene.overlay().showControls(util.vector().topOf(2, 1, 2), Pointing.DOWN, 45).rightClick().withItem(Content.stack("binding_card"));
        text(scene, util, "charging.1", 2, 1, 2); text(scene, util, "charging.2", 2, 1, 2);
        text(scene, util, "charging.3", 4, 1, 3); scene.markAsFinished();
    }
    public static void open(ItemStack stack) {
        if (!PonderIndex.getSceneAccess().doScenesExistForId(stack.getItem().getRegistryName())) stack = Content.stack("book");
        Minecraft.getMinecraft().displayGuiScreen(PonderUI.of(stack));
    }
}
