package owmii.powah.energy;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/** All dimensions use the overworld's data, scoped by owner UUID and channel. */
public final class EnderNetwork extends WorldSavedData {
    private final Map<String, EnergyStore> channels = new HashMap<>();
    public EnderNetwork() { super("powah_ender_network"); }
    public EnderNetwork(String name) { super(name); }
    public static EnderNetwork get(World world) {
        World root = world.getMinecraftServer().getWorld(0);
        var storage = root.getMapStorage();
        EnderNetwork network = (EnderNetwork) storage.getOrLoadData(EnderNetwork.class, "powah_ender_network");
        if (network == null) { network = new EnderNetwork(); storage.setData("powah_ender_network", network); }
        return network;
    }
    public EnergyStore channel(UUID owner, int channel) {
        return channels.computeIfAbsent(owner + ":" + channel, key -> new EnergyStore(0, Integer.MAX_VALUE, this::markDirty));
    }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        channels.clear(); NBTTagList list = nbt.getTagList("Channels", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            EnergyStore store = new EnergyStore(entry.getLong("Capacity"), Integer.MAX_VALUE, this::markDirty);
            store.set(entry.getLong("Energy")); channels.put(entry.getString("Key"), store);
        }
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        channels.forEach((key, store) -> {
            NBTTagCompound entry = new NBTTagCompound(); entry.setString("Key", key);
            entry.setLong("Energy", store.stored()); entry.setLong("Capacity", store.capacity()); list.appendTag(entry);
        });
        nbt.setTag("Channels", list); return nbt;
    }
}
