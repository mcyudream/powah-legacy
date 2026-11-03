package owmii.powah.block;

import java.util.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.energy.*;
import owmii.powah.energy.EnergyStore;

/** Routing is ticked once per loaded component; every path honors its slowest cable. */
public final class CableTile extends MachineTile {
    private long networkTick = Long.MIN_VALUE;
    private int remaining;
    private final Map<BlockPos, Long> recentInputs = new HashMap<>();
    @Override public IEnergyStorage energyView(EnumFacing side) {
        IEnergyStorage storage = super.energyView(side);
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) {
                int moved = storage.receiveEnergy(amount, simulate);
                if (moved > 0 && !simulate && side != null && world != null) recentInputs.put(pos.offset(side), world.getTotalWorldTime());
                return moved;
            }
            @Override public int extractEnergy(int amount, boolean simulate) { return storage.extractEnergy(amount, simulate); }
            @Override public int getEnergyStored() { return storage.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return storage.getMaxEnergyStored(); }
            @Override public boolean canExtract() { return storage.canExtract(); }
            @Override public boolean canReceive() { return storage.canReceive(); }
        };
    }
    private record Route(CableTile cable, List<CableTile> path) { }
    private CableTile neighbor(EnumFacing side) {
        BlockPos next = pos.offset(side);
        if (sides[side.ordinal()] == 3 || !world.isBlockLoaded(next)) return null;
        return world.getTileEntity(next) instanceof CableTile tile && tile.sides[side.getOpposite().ordinal()] != 3 ? tile : null;
    }
    @Override public void update() {
        if (world == null || world.isRemote || networkTick == world.getTotalWorldTime()) return;
        long tick = world.getTotalWorldTime();
        ArrayDeque<CableTile> queue = new ArrayDeque<>(); List<CableTile> network = new ArrayList<>(); Set<BlockPos> seen = new HashSet<>();
        queue.add(this); seen.add(pos);
        while (!queue.isEmpty()) {
            CableTile cable = queue.remove(); cable.networkTick = tick; cable.remaining = cable.enabled() ? cable.tier().transfer("energy_cable") : 0; network.add(cable);
            for (EnumFacing side : EnumFacing.values()) {
                CableTile next = cable.neighbor(side);
                if (next != null && seen.add(next.pos)) queue.add(next);
            }
        }
        // Rotate sources every tick so branches have equal access to a shared bottleneck.
        if (!network.isEmpty()) Collections.rotate(network, (int) (tick % network.size()));
        Set<BlockPos> sources = new HashSet<>();
        for (CableTile cable : network) {
            cable.recentInputs.entrySet().removeIf(entry -> tick - entry.getValue() > 20);
            sources.addAll(cable.recentInputs.keySet());
        }
        for (CableTile source : network) if (source.energy().stored() > 0 && source.remaining > 0) source.route(sources);
        for (CableTile cable : network) if (tick % 20 == 0) cable.sync();
    }
    private void route(Set<BlockPos> sources) {
        ArrayDeque<Route> queue = new ArrayDeque<>(); Set<BlockPos> visited = new HashSet<>(); queue.add(new Route(this, List.of(this))); visited.add(pos);
        while (!queue.isEmpty() && energy().stored() > 0) {
            Route route = queue.remove(); CableTile cable = route.cable();
            int limit = EnergyStore.bounded(energy().stored());
            for (CableTile path : route.path()) limit = Math.min(limit, path.remaining);
            if (limit <= 0) continue;
            int rotation = (int) (world.getTotalWorldTime() % 6);
            for (int i = 0; i < 6 && limit > 0; i++) {
                EnumFacing side = EnumFacing.values()[(i + rotation) % 6];
                if (!cable.outputAllowed(side)) continue;
                BlockPos targetPos = cable.pos.offset(side); if (!world.isBlockLoaded(targetPos)) continue;
                if (sources.contains(targetPos)) continue;
                TileEntity neighbor = world.getTileEntity(targetPos);
                if (neighbor == null || neighbor instanceof CableTile) continue;
                IEnergyStorage target = neighbor.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
                if (target == null || !target.canReceive()) continue;
                int moved = target.receiveEnergy(Math.min(limit, EnergyStore.bounded(energy().stored())), false);
                if (moved > 0) { energy().remove(moved, false); for (CableTile path : route.path()) path.remaining -= moved; limit -= moved; }
            }
            for (EnumFacing side : EnumFacing.values()) {
                CableTile next = cable.neighbor(side);
                if (next != null && cable.outputAllowed(side) && next.inputAllowed(side.getOpposite()) && next.remaining > 0 && visited.add(next.pos)) {
                    List<CableTile> path = new ArrayList<>(route.path()); path.add(next); queue.add(new Route(next, path));
                }
            }
        }
    }
    @Override protected void pushEnergy() { }
}
