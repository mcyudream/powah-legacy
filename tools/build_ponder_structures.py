"""Write vanilla structure NBT without an external Python dependency."""
from pathlib import Path
import gzip
import struct

def string(value):
    data = value.encode('utf-8')
    return struct.pack('>H', len(data)) + data

def payload(typ, value):
    if typ == 1: return struct.pack('>b', value)
    if typ == 3: return struct.pack('>i', value)
    if typ == 4: return struct.pack('>q', value)
    if typ == 6: return struct.pack('>d', value)
    if typ == 8: return string(value)
    if typ == 9:
        subtype, values = value
        return bytes([subtype]) + struct.pack('>i', len(values)) + b''.join(payload(subtype, x) for x in values)
    if typ == 10:
        return b''.join(bytes([t]) + string(k) + payload(t, v) for k, (t, v) in value.items()) + b'\0'
    raise ValueError(typ)

def position(value): return (9, (3, list(value)))

def build_all(destination):
    destination = Path(destination)
    for scene in ['power', 'energizing', 'reactor', 'ender', 'charging']:
        palette, blocks = [], {}
        def add(pos, name, props=None, nbt=None):
            state = {'Name': (8, name)}
            if props: state['Properties'] = (10, {k: (8, str(v).lower()) for k, v in props.items()})
            if state not in palette: palette.append(state)
            block = {'pos': position(pos), 'state': (3, palette.index(state))}
            if nbt is not None: block['nbt'] = (10, nbt)
            blocks[tuple(pos)] = block
        def machine(pos, name, facing='north', **values):
            kind = name if name == 'energizing_orb' else name.rsplit('_', 1)[0]
            tier = 'starter' if name == 'energizing_orb' else name.rsplit('_', 1)[1]
            tile_id = 'energizing_orb' if kind == 'energizing_orb' else 'cable' if kind == 'energy_cable' else 'reactor' if kind == 'reactor' else 'machine'
            nbt = {'id': (8, 'powah:' + tile_id), 'Kind': (8, kind), 'Tier': (8, tier), 'Energy': (4, 50000)}
            nbt.update(values)
            add(pos, 'powah:' + name, {'facing': facing, 'assembled': False}, nbt)
        for x in range(7):
            for z in range(7):
                add((x, 0, z), 'minecraft:quartz_block' if 0 < x < 6 and 0 < z < 6 else 'minecraft:stone')
        if scene == 'power':
            machine((1, 1, 3), 'furnator_basic')
            for x in [2, 3, 4]: machine((x, 1, 3), 'energy_cable_basic')
            machine((5, 1, 3), 'energy_cell_basic')
            for x, kind in [(1, 'solar_panel'), (3, 'magmator'), (5, 'thermo_generator')]: machine((x, 1, 1), kind + '_basic')
            add((5, 0, 1), 'minecraft:magma')
        if scene == 'energizing':
            inv = {'Size': (3, 7), 'Items': (9, (10, [
                {'Slot': (3, 1), 'id': (8, 'minecraft:iron_ingot'), 'Count': (1, 1), 'Damage': (3, 0)},
                {'Slot': (3, 2), 'id': (8, 'minecraft:gold_ingot'), 'Count': (1, 1), 'Damage': (3, 0)}]))}
            machine((3, 1, 3), 'energizing_orb', facing='down', Inventory=(10, inv))
            for x in [2, 4]:
                machine((x, 1, 2), 'energizing_rod_basic'); machine((x, 1, 1), 'energy_cable_basic')
            machine((3, 1, 1), 'energy_cell_basic')
        if scene == 'reactor':
            for x in range(2, 5):
                for y in range(1, 5):
                    for z in range(2, 5):
                        values = {'Assembled': (1, 0), 'Fuel': (6, 800.)}
                        if (x, y, z) != (3, 1, 3): values['Core'] = (4, (3 << 38) | (1 << 26) | 3)
                        machine((x, y, z), 'reactor_basic', **values)
            machine((5, 1, 3), 'energy_cable_basic'); machine((6, 1, 3), 'energy_cell_basic')
        if scene == 'ender':
            machine((1, 1, 3), 'energy_cell_basic'); machine((2, 1, 3), 'ender_cell_basic')
            machine((5, 1, 3), 'ender_gate_basic', facing='east'); machine((6, 1, 3), 'energy_cell_basic')
        if scene == 'charging':
            machine((2, 1, 2), 'player_transmitter_basic'); machine((3, 1, 2), 'energy_cell_basic')
            machine((4, 1, 3), 'energy_hopper_basic', facing='down'); add((4, 0, 3), 'minecraft:chest', {'facing': 'north'})
            machine((2, 1, 4), 'energy_discharger_basic')
        root = {'DataVersion': (3, 1343), 'size': position((7, 6, 7)), 'palette': (9, (10, palette)),
                'blocks': (9, (10, list(blocks.values()))), 'entities': (9, (10, [])), 'author': (8, 'Powah Cleanroom')}
        destination.mkdir(parents=True, exist_ok=True)
        (destination / (scene + '.nbt')).write_bytes(gzip.compress(b'\x0a\0\0' + payload(10, root), mtime=0))

if __name__ == '__main__':
    build_all(Path(__file__).resolve().parents[1] / 'src/main/resources/assets/powah/ponder')
