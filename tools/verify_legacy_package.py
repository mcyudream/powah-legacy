#!/usr/bin/env python3
"""Check release metadata; this does not replace runtime compatibility acceptance."""
import argparse
import json
import hashlib
from pathlib import Path
from zipfile import ZipFile
p = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--require-verified-hash', action='store_true',
                    help='Require this exact production jar to have passed the four baseline runtime combinations')
args = parser.parse_args()
identity = json.loads((p/'legacy-identity.json').read_text())
jars = [f for f in (p/'build/libs').glob(identity['modid']+'-*.jar') if not f.stem.endswith(('-sources','-api','-mcp','-javadoc'))]
if len(jars) != 1:
    raise SystemExit('Expected exactly one production jar: '+repr(jars))
with ZipFile(jars[0]) as archive:
    metadata = json.loads(archive.read('mcmod.info'))[0]
    assert metadata['modid'] == identity['modid'], metadata
    assert metadata['name'] == identity['name'], metadata
    assert metadata['authorList'] == ['yudream'], metadata
    assert metadata['dependencies'] == ['cleanroom@[0.4.0,)'], metadata
    assert identity['release_tag'] == 'v' + metadata['version'], 'Tag differs from packaged version'
    logo = metadata['logoFile']
    assert logo in archive.namelist(), 'Missing project icon: ' + logo
    assert archive.read(logo) == (p/'branding/icon.png').read_bytes(), 'Packaged icon differs from project artwork'
    assert '${' not in archive.read('mcmod.info').decode(), 'Unexpanded metadata'
    assert not any('/dev/' in name or 'Smoke.class' in name for name in archive.namelist() if name.endswith('.class')), 'Development harness in release'
    assert not any(name.startswith(('net/createmod/', 'com/simibubi/create/', 'crafttweaker/'))
                   for name in archive.namelist() if name.endswith('.class')), 'Optional dependency bundled in release'
    runtime = json.loads(archive.read('runtime-lock.json'))
    assert runtime['cleanroom_minimum'] == '0.4.0'
    assert runtime['ponder']['optional'] and runtime['ponder']['modid'] == 'ponder-legacy'
    manifest = archive.read('META-INF/MANIFEST.MF').decode().replace('\r\n ', '').replace('\n ', '')
    assert 'FMLCorePlugin: owmii.powah.compat.cleanroom.Cleanroom040ServerPlugin' in manifest
sha256 = hashlib.sha256(jars[0].read_bytes()).hexdigest()
if args.require_verified_hash:
    if sha256 != identity['verified_jar_sha256']:
        raise SystemExit('Runtime acceptance required for new jar SHA-256: ' + sha256
                         + '\nAccepted: ' + identity['verified_jar_sha256'])
    evidence = p / identity['verification_directory']
    for variant in ('client-without-ponder', 'client-with-ponder', 'server-without-ponder', 'server-with-ponder'):
        runtime = json.loads((evidence / variant / 'runtime.json').read_text())
        result = json.loads((evidence / variant / 'result.json').read_text())
        report = json.loads((evidence / variant / 'report.json').read_text())
        assert runtime['cleanroom'] == result['cleanroom'] == report['cleanroom'] == '0.4.0', variant
        assert runtime['side'] == variant.split('-')[0], variant
        assert runtime['ponder'] == report['withPonder'] == ('-with-' in variant), variant
        assert runtime['jars'][jars[0].name] == sha256, variant + ': different installation jar'
        assert result['result'] == report['result'] == 'PASS' and result['exit_code'] == 0, variant
        assert report['checks'], variant + ': no runtime checks'
print('PASS:', jars[0].name, metadata['modid'], 'SHA-256:', sha256)
