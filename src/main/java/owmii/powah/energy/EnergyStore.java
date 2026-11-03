package owmii.powah.energy;

import net.minecraftforge.energy.IEnergyStorage;

/** Long internal accounting, with the bounded int view required by Forge Energy. */
public class EnergyStore implements IEnergyStorage {
    public static final long MAX = Long.MAX_VALUE / 4;
    private long stored, capacity;
    private final long transfer;
    private final Runnable changed;
    public EnergyStore(long capacity, long transfer, Runnable changed) {
        this.capacity = Math.max(0, Math.min(MAX, capacity));
        this.transfer = Math.max(0, transfer);
        this.changed = changed;
    }
    public long stored() { return stored; }
    public long capacity() { return capacity; }
    public long transfer() { return transfer; }
    public long empty() { return capacity - stored; }
    public void capacity(long value) {
        long previous = capacity;
        capacity = Math.max(0, Math.min(MAX, value)); set(stored);
        if (capacity != previous) changed.run();
    }
    public void set(long value) {
        long next = Math.max(0, Math.min(capacity, value));
        if (next != stored) { stored = next; changed.run(); }
    }
    public long insert(long amount, boolean simulate) {
        long accepted = Math.max(0, Math.min(amount, empty()));
        if (!simulate) set(stored + accepted);
        return accepted;
    }
    public long remove(long amount, boolean simulate) {
        long removed = Math.max(0, Math.min(amount, stored));
        if (!simulate) set(stored - removed);
        return removed;
    }
    public static int bounded(long value) { return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value)); }
    @Override public int receiveEnergy(int amount, boolean simulate) { return bounded(insert(Math.min(transfer, amount), simulate)); }
    @Override public int extractEnergy(int amount, boolean simulate) { return bounded(remove(Math.min(transfer, amount), simulate)); }
    @Override public int getEnergyStored() { return bounded(stored); }
    @Override public int getMaxEnergyStored() { return bounded(capacity); }
    @Override public boolean canExtract() { return transfer > 0; }
    @Override public boolean canReceive() { return transfer > 0; }
}
