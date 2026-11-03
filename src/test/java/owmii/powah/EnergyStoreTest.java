package owmii.powah;

import org.junit.Test;
import static org.junit.Assert.*;
import owmii.powah.energy.EnergyStore;

public class EnergyStoreTest {
    @Test public void simulationNeverChangesBalances() {
        EnergyStore store = new EnergyStore(1000, 100, () -> { }); store.set(700);
        assertEquals(100, store.receiveEnergy(500, true)); assertEquals(100, store.extractEnergy(500, true)); assertEquals(700, store.stored());
        assertEquals(0, store.receiveEnergy(-1, false)); assertEquals(0, store.extractEnergy(-1, false)); assertEquals(700, store.stored());
    }
    @Test public void longNetworkCapacityHasBoundedForgeView() {
        EnergyStore store = new EnergyStore(10_000_000_000L, 2_000_000, () -> { });
        store.insert(8_000_000_000L, false); assertEquals(8_000_000_000L, store.stored());
        assertEquals(Integer.MAX_VALUE, store.getEnergyStored()); assertEquals(Integer.MAX_VALUE, store.getMaxEnergyStored());
        assertEquals(2_000_000, store.extractEnergy(Integer.MAX_VALUE, false)); assertEquals(7_998_000_000L, store.stored());
        store.capacity(50); assertEquals(50, store.stored()); assertEquals(50, store.remove(Long.MAX_VALUE, false)); assertEquals(0, store.stored());
    }
}
