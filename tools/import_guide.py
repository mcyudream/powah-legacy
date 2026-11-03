#!/usr/bin/env python3
"""Import Powah's complete bilingual GuideME pages and pinned UI assets."""
import hashlib
import json
from pathlib import Path
import posixpath
import re
import urllib.request
from zipfile import ZipFile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = ROOT.parents[1] / 'upstream/Powah/guidebook'
RES = ROOT / 'src/main/resources/assets/powah'


def parse(path, relative):
    source = path.read_text()
    _, front, body = source.split('---', 2)
    fields = dict(re.findall(r'^  (title|parent|icon|position): (.+)$', front, re.M))
    page = {'id': relative, 'title': fields['title'], 'parent': fields.get('parent', '' if relative == 'index.md' else 'index.md'),
            'icon': fields.get('icon', 'powah:book'), 'position': int(fields.get('position', 0)),
            'items': re.findall(r'^  - (powah:\S+)$', front, re.M), 'blocks': []}
    blocks = page['blocks']
    paragraph = []

    def flush():
        if paragraph:
            blocks.append({'type': 'text', 'text': ' '.join(paragraph)})
            paragraph.clear()

    for line in body.splitlines():
        line = line.strip()
        if not line:
            flush()
        elif line.startswith('# '):
            flush()
            page['title'] = line[2:]
        elif match := re.fullmatch(r'!\[.*?\]\((.*?)\)', line):
            flush()
            image = posixpath.normpath(posixpath.join(posixpath.dirname(relative), match[1]))
            with Image.open(UPSTREAM / image) as bitmap:
                w, h = bitmap.size
            blocks.append({'type': 'image', 'image': image, 'width': w, 'height': h})
        elif line.startswith('|'):
            flush()
            cells = [c.strip() for c in line.strip('|').split('|')]
            if all(re.fullmatch(r'[:\- ]+', c) for c in cells):
                continue
            if not blocks or blocks[-1]['type'] != 'table':
                blocks.append({'type': 'table', 'rows': []})
            blocks[-1]['rows'].append(cells)
        elif line.startswith('<RecipesFor'):
            flush()
            item = re.search(r'id="([^"]+)"', line)[1]
            if not blocks or blocks[-1]['type'] != 'recipes':
                blocks.append({'type': 'recipes', 'items': []})
            blocks[-1]['items'].append(item)
        elif line in ('<Row>', '</Row>'):
            flush()
        elif line.startswith('<SubPages'):
            flush()
            blocks.append({'type': 'children'})
        elif match := re.fullmatch(r'- \[(.*?)\]\((.*?)\)', line):
            flush()
            target = posixpath.normpath(posixpath.join(posixpath.dirname(relative), match[2]))
            blocks.append({'type': 'link', 'text': match[1], 'target': target})
        else:
            paragraph.append(line)
    flush()
    return page


def build():
    translations = json.loads((ROOT / 'tools/translations.json').read_text())
    supplements = {'index.md': 'intro', 'generators/index.md': 'power',
                   'energy_blocks/energizing.md': 'energizing', 'generators/reactor.md': 'reactor',
                   'storage_transfer/ender_cell.md': 'ender', 'energy_blocks/index.md': 'charging',
                   'materials/index.md': 'materials'}
    source_pages = sorted(p for p in UPSTREAM.rglob('*.md') if not p.relative_to(UPSTREAM).parts[0].startswith('_'))
    for language in ('en_us', 'zh_cn'):
        pages = []
        for source in source_pages:
            relative = source.relative_to(UPSTREAM).as_posix()
            local = UPSTREAM / '_zh_cn' / relative if language == 'zh_cn' else source
            if not local.is_file():
                raise ValueError(f'Missing {language} guide page: {relative}')
            page = parse(local, relative)
            if relative in supplements:
                text = translations[f'powah.guide.{supplements[relative]}'][language]
                # Preserve the original page and include the executable 1.12 workflows.
                page['blocks'].append({'type': 'heading', 'text': '操作步骤' if language == 'zh_cn' else 'Step by step'})
                page['blocks'] += [{'type': 'text', 'text': p} for p in text.split('\n')]
            pages.append(page)
        dest = RES / 'guide' / f'{language}.json'
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(json.dumps(pages, ensure_ascii=False, indent=2) + '\n')
    for source in UPSTREAM.rglob('*.png'):
        if source.relative_to(UPSTREAM).parts[0].startswith('_'):
            continue
        dest = RES / 'textures/guide' / source.relative_to(UPSTREAM)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(source.read_bytes())
    lock = json.loads((ROOT / 'guide-ui-lock.json').read_text())
    jar = ROOT.parents[1] / '.dev/guideme-21.1.7/guideme-21.1.7.jar'
    if not jar.exists():
        jar.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(lock['artifact'], timeout=30) as response:
            jar.write_bytes(response.read())
    if hashlib.sha256(jar.read_bytes()).hexdigest() != lock['sha256']:
        raise ValueError('GuideME source asset checksum mismatch')
    with ZipFile(jar) as archive:
        for source in lock['imported_assets']:
            name = 'background.png' if source.endswith('background.png') else 'buttons.png'
            (RES / 'textures/guide' / name).write_bytes(archive.read(source))
    print(f'Imported {len(source_pages)} GuideME pages in English and Simplified Chinese.')


if __name__ == '__main__':
    build()
