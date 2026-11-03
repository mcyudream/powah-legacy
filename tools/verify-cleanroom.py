#!/usr/bin/env python3
"""Verify production jars on official Cleanroom 0.4.0, without a development resource pack (Linux x64)."""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import urllib.request
from zipfile import ZipFile

PROJECT = Path(__file__).resolve().parent.parent
WORKSPACE = PROJECT.parent.parent
VERSION = "0.4.0"
CACHE = WORKSPACE / ".dev" / f"cleanroom-{VERSION}-runtime"
GRADLE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
PACK = WORKSPACE / ".dev" / f"cleanroom-{VERSION}-installer.jar"
PACK_SHA256 = "882435d851a51e14b802e0a9384e4a05cfbb3890fca435c40c494af75e5a166f"


def digest(path, algorithm="sha1"):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, algorithm).hexdigest()


def download(url, destination, expected, algorithm="sha1", candidates=()):
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.is_file() and digest(destination, algorithm) == expected:
        return destination
    for candidate in candidates:
        if candidate.is_file() and digest(candidate, algorithm) == expected:
            shutil.copyfile(candidate, destination)
            return destination
    urls = [url]
    if url.startswith("https://repo.cleanroommc.com/releases/"):
        urls.insert(0, url.replace("https://repo.cleanroommc.com/releases/", "https://maven.cleanroommc.com/"))
    last_error = None
    for attempt in range(2):
        for source in urls:
            try:
                request = urllib.request.Request(source, headers={"User-Agent": "HNN-Cleanroom-verification"})
                partial = destination.with_name(destination.name + ".part")
                with urllib.request.urlopen(request, timeout=30) as response, partial.open("wb") as output:
                    shutil.copyfileobj(response, output, 128 * 1024)
                if digest(partial, algorithm) != expected:
                    raise ValueError(f"Hash mismatch: {destination.name}")
                partial.replace(destination)
                print("Downloaded", destination.name, flush=True)
                return destination
            except (OSError, ValueError) as error:
                last_error = error
    raise RuntimeError(f"Could not download {url}: {last_error}")


def allowed(library):
    rules = library.get("rules", [])
    result = not rules
    for rule in rules:
        os_rule = rule.get("os", {})
        if os_rule.get("name", "linux") == "linux" and os_rule.get("arch", "x86_64") in ("x86_64", "amd64"):
            result = rule["action"] == "allow"
    # The historical MMC recipe also lists ARM natives with a plain Linux rule.
    classifier = library["name"].split(":")[3:]
    return result and (not classifier or not classifier[0].startswith("natives-") or classifier == ["natives-linux"])


def artifact(library):
    entry = library["downloads"]["artifact"]
    parts = library["name"].split(":")
    group, module, version = parts[:3]
    path = entry.get("path") or f"{group.replace('.', '/')}/{module}/{version}/{entry['url'].rsplit('/', 1)[1]}"
    candidates = (GRADLE / "caches/modules-2/files-2.1" / group / module / version).glob(f"*/{Path(path).name}")
    candidates = list(candidates)
    if module == "cleanroom":
        candidates.append(WORKSPACE / ".dev" / f"cleanroom-{VERSION}-universal.jar")
    # Reuse byte-identical libraries from the old verification cache only after SHA-1 validation.
    candidates += list((WORKSPACE / ".dev/cleanroom-0.4.0-alpha-runtime/libraries").glob("**/" + Path(path).name))
    if module == "cleanroom":
        entry = dict(entry, url=f"https://maven.cleanroommc.com/com/cleanroommc/cleanroom/{VERSION}/cleanroom-{VERSION}-universal.jar")
    return download(entry["url"], CACHE / "libraries" / path, entry["sha1"], candidates=candidates)


def prepare(side):
    download(f"https://maven.cleanroommc.com/com/cleanroommc/cleanroom/{VERSION}/{PACK.name}", PACK, PACK_SHA256, "sha256")
    with ZipFile(PACK) as archive:
        recipe = json.loads(archive.read("version.json"))
    if recipe["id"] != f"cleanroom-{VERSION}":
        raise ValueError("Unexpected Cleanroom version in the official installer")
    metadata = json.loads((GRADLE / "caches/cleanroom_gradle/versions/1.12.2/meta.json").read_text())
    # Cleanroom replaces vanilla's obsolete LWJGL, Netty, ICU, JNA, logging and utility libraries.
    # Retain only the vanilla audio, authentication and platform services used by the official recipe.
    retained = {"com.paulscode", "com.mojang:authlib", "com.mojang:realms", "com.mojang:text2speech", "ca.weblite:java-objc-bridge"}
    vanilla_libraries = [library for library in metadata["libraries"]
                        if library["name"].split(":")[0] in retained or ":".join(library["name"].split(":")[:2]) in retained]
    libraries = {}
    for group in (recipe["libraries"], vanilla_libraries):
        for library in group:
            if allowed(library):
                libraries.setdefault(library["name"], library)
                native = library.get("natives", {}).get("linux")
                if native:
                    libraries.setdefault(library["name"] + ":" + native, {
                        "name": library["name"] + ":" + native,
                        "downloads": {"artifact": library["downloads"]["classifiers"][native]},
                    })
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        classpath = list(pool.map(artifact, libraries.values()))
    main = metadata["downloads"][side]
    vanilla = GRADLE / f"caches/cleanroom_gradle/versions/1.12.2/{side}.jar"
    classpath.append(download(main["url"], CACHE / f"minecraft-{side}-1.12.2.jar", main["sha1"], candidates=[vanilla]))
    # Keep loaded ELF files private to this run; another verifier may prepare
    # the shared download cache while this JVM still maps its native libraries.
    natives = PROJECT / "build/verification-natives" / str(os.getpid())
    natives.mkdir(parents=True, exist_ok=True)
    for jar in classpath:
        if "natives-linux" in jar.name:
            with ZipFile(jar) as archive:
                for name in archive.namelist():
                    if name.endswith(".so"):
                        target = natives / Path(name).name
                        data = archive.read(name)
                        if not target.is_file() or target.read_bytes() != data:
                            partial = target.with_name(target.name + f".{os.getpid()}.part")
                            partial.write_bytes(data)
                            partial.replace(target)
    return classpath, natives



def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('side', choices=['client', 'server'])
    parser.add_argument('--with-ponder', action='store_true')
    parser.add_argument('--empty', action='store_true', help='Reproduce loader failures with an empty server')
    parser.add_argument('--prepare-only', action='store_true')
    args = parser.parse_args()
    if platform.system() != 'Linux' or platform.machine() not in ('x86_64', 'amd64'):
        parser.error('This verification launcher supports Linux x64')
    if args.empty and (args.side != 'server' or args.with_ponder):
        parser.error('--empty requires a server without Ponder')
    classpath, natives = prepare(args.side)
    if args.prepare_only:
        return
    lock = json.loads((PROJECT / 'runtime-lock.json').read_text())
    universal = next(jar for jar in classpath if jar.name == f'cleanroom-{VERSION}.jar')
    if digest(universal, 'sha256') != lock['cleanroom_runtime']['universal_sha256']:
        raise ValueError('The official Cleanroom installation jar checksum differs')
    variant = 'empty-server' if args.empty else f'{args.side}-{"with" if args.with_ponder else "without"}-ponder'
    run = PROJECT / 'run' / f'cleanroom-{VERSION}-legacy' / variant
    mods = run / 'mods'
    if mods.exists():
        shutil.rmtree(mods)
    mods.mkdir(parents=True)
    version = (PROJECT / 'build.gradle').read_text().split("version = '", 1)[1].split("'", 1)[0]
    release = PROJECT / f'build/libs/powah-legacy-{version}.jar'
    harness = 'powah-verification-srg.jar' if args.side == 'client' else 'powah-server-verification-srg.jar'
    jars = [] if args.empty else [release, PROJECT / 'build/dev' / harness]
    if args.with_ponder:
        craft = list((GRADLE / 'caches/modules-2/files-2.1/maven.modrinth/crafttweaker/4.1.20.698').glob('*/*.jar'))
        if len(craft) != 1 or digest(craft[0], 'sha256') != lock['crafttweaker']['sha256']:
            raise ValueError('Build with the pinned CraftTweaker 4.1.20.698 first')
        ponder = Path(os.environ.get('LEGACY_PONDER_JAR', PROJECT.parent / 'ponder/build/libs/ponder-legacy-1.4.0-cleanroom-alpha.1.jar'))
        jars += [ponder, craft[0]]
    for jar in jars:
        if not jar.is_file():
            raise FileNotFoundError(f'Build the installation jars and reobfuscated verification harness first: {jar}')
        shutil.copyfile(jar, mods / jar.name)
    config = run / 'config'
    config.mkdir(exist_ok=True)
    (config / 'splash.properties').write_text('enabled=false\n')
    output = run / ('powah-smoke' if args.side == 'client' else 'server-verification')
    if output.exists():
        shutil.rmtree(output)
    output.mkdir()
    server_report = run / 'powah-server-smoke.json'
    server_report.unlink(missing_ok=True)
    jdk = Path(os.environ.get('POWAH_JAVA_HOME', WORKSPACE / '.dev/jdk-25.0.5+8'))
    command = [str(jdk / 'bin/java'), '-Xms256m', '-Xmx1536m', '-XX:ActiveProcessorCount=2',
               '-Dfile.encoding=UTF-8', '--enable-native-access=ALL-UNNAMED',
               f'-Djava.library.path={natives}', '-Dpowah.dev.smoke=true',
               f'-Dpowah.expectedCleanroom={VERSION}', '-cp', os.pathsep.join(map(str, classpath))]
    if args.side == 'client':
        command += ['top.outlands.foundation.boot.Foundation', '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker',
                    '--username', 'PowahDeveloper', '--uuid', '8b543c6b-0aaf-3b93-bf4a-e15e899058c8', '--accessToken', '0',
                    '--version', f'Cleanroom-{VERSION}', '--gameDir', str(run),
                    '--assetsDir', str(GRADLE / 'caches/cleanroom_gradle/assets'), '--assetIndex', '1.12',
                    '--width', '1280', '--height', '720']
    else:
        eula = run / 'eula.txt'
        accepted = PROJECT / 'run/server-standalone/eula.txt'
        # The user has accepted the EULA for this project's local test server.
        if not eula.exists() and accepted.exists():
            shutil.copyfile(accepted, eula)
        if not eula.exists() or 'eula=true' not in eula.read_text().splitlines():
            parser.error(f'Minecraft EULA acceptance is required in {eula}')
        (run / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nview-distance=2\nlevel-type=FLAT\n')
        command += ['net.minecraftforge.fml.relauncher.ServerLaunchWrapper', 'nogui']
    environment = os.environ.copy()
    if args.side == 'client':
        environment['ALSOFT_DRIVERS'] = 'null'
    runtime = {'cleanroom': VERSION, 'side': args.side, 'ponder': args.with_ponder, 'empty': args.empty,
               'installer_sha256': PACK_SHA256, 'jars': {jar.name: digest(jar, 'sha256') for jar in jars},
               'libraries': {str(jar.relative_to(CACHE)): digest(jar) for jar in classpath},
               'audio_backend': 'null' if args.side == 'client' else None,
               'isolated_natives': {p.name: digest(p, 'sha256') for p in natives.glob('*.so')}}
    (output / 'runtime.json').write_text(json.dumps(runtime, indent=2) + '\n')
    print(f'Starting official Cleanroom {VERSION}: {variant}; installation jars, no userdev remapping', flush=True)
    with (output / 'startup.log').open('w') as log:
        try:
            process = subprocess.run(command, cwd=run, env=environment, stdout=log, stderr=subprocess.STDOUT, timeout=600 if not args.empty else 90)
            exit_code = process.returncode
        except subprocess.TimeoutExpired:
            exit_code = None
    report = output / 'report.json' if args.side == 'client' else server_report
    if args.side == 'server' and report.is_file():
        shutil.copyfile(report, output / 'report.json')
    passed = exit_code == 0 and report.is_file() and json.loads(report.read_text()).get('result') == 'PASS'
    result = {'result': 'PASS' if passed else 'FAIL', 'exit_code': exit_code, 'cleanroom': VERSION, 'variant': variant,
              'report_created': report.is_file()}
    if not passed:
        log = (output / 'startup.log').read_text(errors='replace')
        if 'EntityPlayerSP' in log and 'Loader.identifyMods(Loader.java:379)' in log:
            result['failure'] = 'Official Cleanroom 0.4.0 initializes Kirino client classes on the dedicated server'
        elif report.exists():
            result['failure'] = json.loads(report.read_text()).get('error', 'Verification checks failed')
        else:
            result['failure'] = 'The loader did not reach the verification mod; inspect startup.log'
    (output / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    destination = PROJECT / 'build/reports' / f'cleanroom-{VERSION}-legacy' / variant
    if destination.exists():
        shutil.rmtree(destination)
    shutil.copytree(output, destination)
    print(json.dumps(result, ensure_ascii=False), flush=True)
    print('Evidence:', destination, flush=True)
    if not passed:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
