package owmii.powah.block;

import java.util.Locale;

public enum Tier {
    STARTER(1, 1, 1), BASIC(4, 3, 2), HARDENED(10, 5, 3), BLAZING(40, 10, 5),
    NIOTIC(100, 20, 7), SPIRITED(400, 40, 9), NITRO(2000, 100, 12), CREATIVE(2000, 100, 12);
    public final int scale, passive, channels;
    Tier(int scale, int passive, int channels) { this.scale = scale; this.passive = passive; this.channels = channels; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public static Tier of(String name) { return name.isEmpty() ? STARTER : valueOf(name.toUpperCase(Locale.ROOT)); }
    public long capacity(String kind) {
        long value = switch (kind) {
            case "energy_cable" -> 500L * scale;
            case "furnator", "magmator", "thermo_generator", "solar_panel" -> 20_000L * scale;
            case "reactor" -> 250_000L * scale;
            case "energizing_rod" -> 10_000L * scale;
            case "energizing_orb", "ender_cell", "ender_gate" -> 0;
            default -> 1_000_000L * scale;
        };
        return this == CREATIVE ? 2_000_000_000L : value;
    }
    public int transfer(String kind) {
        return switch (kind) {
            case "furnator", "magmator", "thermo_generator", "solar_panel" -> 80 * scale;
            case "reactor", "battery", "energy_cell", "ender_cell", "energy_hopper", "energy_discharger", "player_transmitter" -> 1000 * scale;
            case "energizing_rod" -> 100 * scale;
            default -> 500 * scale;
        };
    }
    public int production(String kind) {
        return switch (kind) {
            case "reactor" -> 250 * scale;
            case "solar_panel", "thermo_generator" -> 20 * passive;
            default -> 20 * scale;
        };
    }
}
