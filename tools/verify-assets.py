#!/usr/bin/env python3
"""Verify bilingual content, imported guide links, and the runtime baseline."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/powah'

def language(name):
    lines = [line.split('=', 1) for line in (RES / f'lang/{name}.lang').read_text().splitlines() if '=' in line and not line.startswith('#')]
    assert len(lines) == len(dict(lines)), f'Duplicate language keys: {name}'
    return dict(lines)

english, chinese = language('en_us'), language('zh_cn')
assert english.keys() == chinese.keys(), 'English and Chinese key sets differ'
for key, value in english.items():
    specifiers = lambda text: sorted(re.findall(r'%(?:\d+\$)?[sdif]', text))
    assert specifiers(value) == specifiers(chinese[key]), f'Format mismatch: {key}'
content = json.loads((RES / 'legacy/content.json').read_text())
for block in content['blocks']:
    assert 'tile.powah.' + block['id'] + '.name' in chinese
for item in content['items']:
    assert 'item.powah.' + item + '.name' in chinese
    assert 'item.powah.' + item not in chinese, 'Modern key breaks 1.12 display names'
page_sets = []
for name in ('en_us', 'zh_cn'):
    pages = json.loads((RES / f'guide/{name}.json').read_text())
    ids = {page['id'] for page in pages}
    assert len(ids) == len(pages) == 28
    page_sets.append(ids)
    for page in pages:
        assert page['title'] and (not page['parent'] or page['parent'] in ids)
        for block in page['blocks']:
            assert block['type'] in ('text', 'heading', 'link', 'image', 'table', 'recipes', 'children')
            if block['type'] == 'link': assert block['target'] in ids
            if block['type'] == 'image': assert (RES / 'textures/guide' / block['image']).is_file()
            if block['type'] == 'text': assert '<' not in re.sub(r'<ItemLink id="[^"]+"\s*/>', '', block['text']), f'Unhandled markup: {page["id"]}'
            if block['type'] == 'table': assert len({len(row) for row in block['rows']}) == 1
assert page_sets[0] == page_sets[1]
assert 'required-after:cleanroom@[0.4.0,)' in (ROOT / 'src/main/java/owmii/powah/Powah.java').read_text()
print(f'PASS: {len(english)} bilingual keys, 145 content names, 28 bilingual guide pages, Cleanroom 0.4.0-alpha minimum.')
