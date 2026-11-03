package owmii.powah.inventory;

/** Common coordinates copied from the pinned upstream containers and GUI textures. */
public enum MachineLayout {
    ENERGY_CELL("energy_cell", 176, 141, 59, new int[][]{{4, 4}, {4, 29}}),
    ENDER("ender_cell", 176, 164, 82, new int[][]{{0, 1000}, {4, 4}, {4, 29}}),
    FURNATOR("furnator", 176, 166, 84, new int[][]{{4, 54}, {87, 18}}),
    MAGMATOR("magmator", 176, 166, 84, new int[][]{{4, 54}}),
    THERMO("thermo", 176, 166, 84, new int[][]{{4, 54}}),
    REACTOR("reactor", 176, 166, 84, new int[][]{{4, 54}, {73, 29}, {31, 6}, {31, 52}, {120, 52}}),
    TRANSMITTER("player_transmitter", 176, 141, 59, new int[][]{{4, 29}}),
    DISCHARGER("discharger", 176, 166, 84, new int[][]{{5, 54}, {30, 54}, {55, 54}, {80, 54}, {105, 54}, {130, 54}, {155, 54}}),
    WIDE("wide_energy", 176, 141, 59, new int[0][]),
    CABLE("cable", 153, 29, -1, new int[0][]);

    public final String texture;
    public final int width, height, playerY;
    private final int[][] slots;

    MachineLayout(String texture, int width, int height, int playerY, int[][] slots) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.playerY = playerY;
        this.slots = slots;
    }

    public int slotX(int slot) { return slots[slot][0]; }
    public int slotY(int slot) { return slots[slot][1]; }

    public static MachineLayout forKind(String kind) {
        return switch (kind) {
            case "energy_cell" -> ENERGY_CELL;
            case "ender_cell", "ender_gate" -> ENDER;
            case "furnator" -> FURNATOR;
            case "magmator" -> MAGMATOR;
            case "thermo_generator" -> THERMO;
            case "reactor" -> REACTOR;
            case "player_transmitter" -> TRANSMITTER;
            case "energy_discharger", "energizing_orb" -> DISCHARGER;
            case "energy_cable" -> CABLE;
            default -> WIDE;
        };
    }
}
