#!/usr/bin/env python3
"""Reproducible asset/recipe conversion from the pinned Powah checkout."""
from pathlib import Path
import json
import re
import shutil
import subprocess
import zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = ROOT.parents[1] / 'upstream/Powah'
RES = ROOT / 'src/main/resources'
LOCK = json.loads((ROOT / 'upstream-lock.json').read_text())
assert subprocess.check_output(['git', '-C', str(UPSTREAM), 'rev-parse', 'HEAD'], text=True).strip() == LOCK['commit']
TIERS = ['starter', 'basic', 'hardened', 'blazing', 'niotic', 'spirited', 'nitro']
KINDS = ['energy_cell', 'ender_cell', 'energy_cable', 'ender_gate', 'energizing_rod', 'furnator', 'magmator',
         'thermo_generator', 'solar_panel', 'reactor', 'player_transmitter', 'energy_hopper', 'energy_discharger']

def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n')

for namespace in ['powah', 'lollipop']:
    source = UPSTREAM / 'src/main/resources/assets' / namespace
    for directory in ['textures', 'models', 'blockstates']:
        if (source / directory).exists():
            shutil.copytree(source / directory, RES / 'assets' / namespace / directory, dirs_exist_ok=True)

# 1.12 can resolve arbitrary texture directories; keep upstream block/item paths.
vanilla_refs = set()
for model in (RES / 'assets').glob('*/models/**/*.json'):
    obj = json.loads(model.read_text())
    if obj.get('parent') == 'block': obj['parent'] = 'block/block'
    for key, value in obj.get('textures', {}).items():
        if value.startswith('minecraft:block/'):
            texture = value.split('/')[-1]
            obj['textures'][key] = 'powah:legacy/' + texture
            vanilla_refs.add(texture)
    write(model, obj)

vanilla_jar = ROOT.parents[1] / '.dev/minecraft-1.21.1-client.jar'
with zipfile.ZipFile(vanilla_jar) as jar:
    for name in sorted(vanilla_refs | {'blue_ice'}):
        dest = RES / ('assets/powah/textures/legacy/' + name + '.png')
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(jar.read('assets/minecraft/textures/block/' + name + '.png'))

blocks = [{'id': f'{kind}_{tier}', 'kind': kind, 'tier': tier} for kind in KINDS for tier in TIERS]
blocks += [{'id': 'energy_cell_creative', 'kind': 'energy_cell', 'tier': 'creative'},
           {'id': 'energizing_orb', 'kind': 'energizing_orb', 'tier': 'starter'}]
solid = ['energized_steel_block', 'blazing_crystal_block', 'niotic_crystal_block', 'spirited_crystal_block',
         'nitro_crystal_block', 'uraninite_block', 'uraninite_ore_poor', 'uraninite_ore', 'uraninite_ore_dense',
         'deepslate_uraninite_ore_poor', 'deepslate_uraninite_ore', 'deepslate_uraninite_ore_dense', 'dry_ice', 'blue_ice']
blocks += [{'id': x, 'kind': 'ore' if '_ore' in x else 'solid'} for x in solid]
items = re.findall(r'DR\.register\("([^"\n]+)"', (UPSTREAM / 'src/main/java/owmii/powah/item/Itms.java').read_text())
items += ['battery_' + t for t in TIERS]
write(RES / 'assets/powah/legacy/content.json', {'blocks': blocks, 'items': items})

for entry in blocks:
    name, kind = entry['id'], entry['kind']
    if kind == 'solar_panel':
        # Four connected panel frames are cosmetic; retain the original central model.
        write(RES / f'assets/powah/blockstates/{name}.json', {'variants': {'normal': {'model': 'powah:block/' + name}}})
    elif kind == 'magmator':
        obj = json.loads((RES / f'assets/powah/blockstates/{name}.json').read_text())
        variants = {key.replace(',lit=false', ''): value
            for key, value in obj['variants'].items() if key.endswith(',lit=false')}
        # The common legacy machine state also permits vertical wrench rotations.
        for facing in ['up', 'down']:
            variants['facing=' + facing] = variants['facing=north'].copy()
        write(RES / f'assets/powah/blockstates/{name}.json', {'variants': variants})
    elif kind == 'player_transmitter':
        obj = json.loads((RES / f'assets/powah/blockstates/{name}.json').read_text())
        write(RES / f'assets/powah/blockstates/{name}.json', {'variants': {'normal': obj['variants']['top=false']}})
    elif kind not in ['energy_cable']:
        path = RES / f'assets/powah/blockstates/{name}.json'
        if path.exists():
            obj = json.loads(path.read_text())
            if '' in obj.get('variants', {}): obj['variants']['normal'] = obj['variants'].pop('')
            write(path, obj)
    if kind == 'reactor':
        # Standalone part model, using the original 64x32 ModelBox UV layout.
        faces = {
            'north': {'uv': [4, 8, 8, 16], 'texture': '#all'},
            'south': {'uv': [12, 8, 16, 16], 'texture': '#all'},
            'east': {'uv': [0, 8, 4, 16], 'texture': '#all'},
            'west': {'uv': [8, 8, 12, 16], 'texture': '#all'},
            'up': {'uv': [4, 0, 8, 8], 'texture': '#all'},
            'down': {'uv': [8, 0, 12, 8], 'texture': '#all'}
        }
        # Tile renderers use rectangular textures. Atlas sprites in 1.12 must be square.
        original = Image.open(RES / f'assets/powah/textures/model/tile/reactor_block_{entry["tier"]}.png').convert('RGBA')
        square = Image.new('RGBA', (original.width, original.width))
        square.paste(original, (0, 0))
        square.save(RES / f'assets/powah/textures/legacy/reactor_block_{entry["tier"]}.png')
        for face in faces.values():
            face['uv'][1] /= 2
            face['uv'][3] /= 2
        write(RES / f'assets/powah/models/block/{name}.json', {'parent': 'block/block',
              'textures': {'all': 'powah:legacy/reactor_block_' + entry['tier'],
                           'particle': 'powah:legacy/reactor_block_' + entry['tier']},
              'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces}]})
        write(RES / f'assets/powah/models/item/{name}.json', {'parent': 'powah:block/' + name})
        write(RES / f'assets/powah/blockstates/{name}.json', {'variants': {
            'assembled=false': {'model': 'powah:block/' + name}, 'assembled=true': {'model': 'lollipop:block/empty'}}})

write(RES / 'assets/powah/blockstates/blue_ice.json', {'variants': {'normal': {'model': 'powah:block/blue_ice'}}})
write(RES / 'assets/powah/models/block/blue_ice.json', {'parent': 'block/cube_all', 'textures': {'all': 'powah:legacy/blue_ice'}})
write(RES / 'assets/powah/models/item/blue_ice.json', {'parent': 'powah:block/blue_ice'})

# The legacy blockstate loader prepends block/ itself; item model parents do not.
def legacy_block_models(value):
    if isinstance(value, dict):
        for key, child in value.items():
            if key == 'model' and isinstance(child, str):
                value[key] = child.replace(':block/', ':', 1)
            else: legacy_block_models(child)
    elif isinstance(value, list):
        for child in value: legacy_block_models(child)
for path in (RES / 'assets/powah/blockstates').glob('*.json'):
    obj = json.loads(path.read_text()); legacy_block_models(obj); write(path, obj)
# Upstream ships stale animation metadata for a now-static Ender Core icon.
for meta in (RES / 'assets').glob('*/textures/**/*.png.mcmeta'):
    obj = json.loads(meta.read_text()); animation = obj.get('animation', {})
    image = Image.open(str(meta)[:-7]); frames = animation.get('frames', [])
    if any((frame if isinstance(frame, int) else frame['index']) >= image.height // image.width for frame in frames): meta.unlink()

TAG_ORES = {'minecraft:coals': 'coal', 'c:glass_blocks': 'blockGlass', 'c:glass_panes': 'paneGlass',
    'c:storage_blocks/quartz': 'blockQuartz', 'c:ingots/iron': 'ingotIron', 'c:ingots/gold': 'ingotGold',
    'c:ingots/uranium': 'ingotUranium', 'c:gems/diamond': 'gemDiamond', 'c:gems/emerald': 'gemEmerald',
    'c:rods/blaze': 'rodBlaze', 'c:nether_stars': 'netherStar', 'c:storage_blocks/redstone': 'blockRedstone',
    'c:ices/blue': 'iceBlue', 'c:ores/uraninite_poor': 'oreUraninitePoor',
    'c:ores/uraninite_regular': 'oreUraninite', 'c:ores/uraninite_dense': 'oreUraniniteDense'}
def ingredient(obj):
    if isinstance(obj, list): return [ingredient(i) for i in obj]
    if 'tag' in obj: return {'type': 'forge:ore_dict', 'ore': TAG_ORES[obj['tag']]}
    result = dict(obj)
    if result.get('item') == 'minecraft:lapis_lazuli': result.update(item='minecraft:dye', data=4)
    return result

recipe_count, energizing = 0, []
for path in sorted((UPSTREAM / 'src/generated/resources/data/powah/recipe').rglob('*.json')):
    obj = json.loads(path.read_text())
    name = str(path.relative_to(UPSTREAM / 'src/generated/resources/data/powah/recipe').with_suffix(''))
    typ = obj['type']
    obj.pop('category', None)
    obj.pop('neoforge:conditions', None)
    if 'result' in obj:
        obj['result']['item'] = obj['result'].pop('id')
    if 'ingredients' in obj: obj['ingredients'] = [ingredient(i) for i in obj['ingredients']]
    if 'key' in obj: obj['key'] = {k: ingredient(v) for k, v in obj['key'].items()}
    if typ == 'powah:energizing':
        obj['id'] = 'powah:' + name
        energizing.append(obj)
    elif typ in ['minecraft:crafting_shaped', 'minecraft:crafting_shapeless']:
        obj['type'] = 'forge:ore_shaped' if typ.endswith('_shaped') else 'forge:ore_shapeless'
        write(RES / f'assets/powah/recipes/{name}.json', obj)
        recipe_count += 1
write(RES / 'assets/powah/legacy/energizing.json', energizing)
write(RES / 'assets/powah/recipes/blue_ice.json', {'type': 'minecraft:crafting_shaped',
    'pattern': ['iii', 'iii', 'iii'], 'key': {'i': {'item': 'minecraft:packed_ice'}},
    'result': {'item': 'powah:blue_ice'}})

for language in ['en_us', 'zh_cn']:
    values = {}
    for ns in ['lollipop', 'powah']:
        values.update(json.loads((UPSTREAM / f'src/main/resources/assets/{ns}/lang/{language}.json').read_text()))
    for entry in blocks:
        name = entry['id']
        values['tile.powah.' + name + '.name'] = values.get('block.powah.' + name, name.replace('_', ' ').title())
    for name in items:
        values['item.powah.' + name + '.name'] = values.get('item.powah.' + name, name.replace('_', ' ').title())
        # 1.12 first translates the base key, then appends .name. Keeping both
        # modern and legacy keys produces translated text with a literal .name.
        values.pop('item.powah.' + name, None)
    values['tile.powah.blue_ice.name'] = '蓝冰' if language == 'zh_cn' else 'Blue Ice'
    values['itemGroup.powah'] = 'Powah!'
    extra = ROOT / 'tools/translations.json'
    if extra.exists(): values.update({k: v[language] for k, v in json.loads(extra.read_text()).items()})
    # Each item has a unique registration id while sharing one of five tutorials.
    for entry in blocks:
        if 'tier' not in entry: continue
        kind = entry['kind']
        group = ('energizing' if kind in ['energizing_orb', 'energizing_rod'] else
                 'reactor' if kind == 'reactor' else
                 'ender' if kind in ['ender_cell', 'ender_gate'] else
                 'charging' if kind in ['energy_hopper', 'energy_discharger', 'player_transmitter'] else 'power')
        values[f'powah.ponder.{group}_{entry["id"]}.header'] = values[f'powah.ponder.{group}.header']
    for name in items:
        group = 'power' if name == 'book' else 'charging' if name.startswith('battery_') else 'energizing'
        values[f'powah.ponder.{group}_{name}.header'] = values[f'powah.ponder.{group}.header']
    dest = RES / f'assets/powah/lang/{language}.lang'
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text('\n'.join(k + '=' + str(v).replace('\n', '\\n') for k, v in sorted(values.items())) + '\n')

write(ROOT / 'docs/import-report.json', {'upstream': LOCK, 'blocks': len(blocks), 'items': len(items),
    'craftingRecipes': recipe_count + 1, 'energizingRecipes': len(energizing),
    'adaptations': ['Common item tags map to the Forge 1.12 ore dictionary.',
      'Lapis lazuli maps to minecraft:dye metadata 4.', 'Blue ice is backfilled as powah:blue_ice with its vanilla nine-packed-ice recipe.',
      'Deepslate ores remain available; natural ores generate in legacy stone at adjusted positive heights.',
      'The duplicate blast-furnace recipe is omitted; the furnace recipe remains.',
      'All 28 bilingual GuideME pages use an adapted native GuideME reading layout and optional Ponder scenes.']})
print(f'Imported {len(blocks)} blocks, {len(items)} items, {recipe_count + 1} crafting and {len(energizing)} energizing recipes.')
from build_ponder_structures import build_all
build_all(RES / 'assets/powah/ponder')
from import_guide import build as build_guide
build_guide()
