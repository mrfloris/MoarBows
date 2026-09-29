#!/usr/bin/env python3
"""Test a release JAR twice on pinned, isolated Paper servers with Java 25."""

import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import urllib.request
import zipfile


BUILDS = {
    "26.2": (129, "b1d8f6bfa1b6101fa8e947b53041cb3bdf5540e7b83b6547ca19ba7edefeb083", 25584),
    "26.3": (134, "16c5494aed1015de4e7c6e5aefece74e0b398f733bd3fcd8975c820598c19bca", 25585),
}
ROOT = Path(__file__).resolve().parents[1]


def build_probe(server, plugin, java):
    output = server / "smoke-classes"
    output.mkdir(exist_ok=True)
    dependencies = sorted((server / "libraries").rglob("*.jar"))
    # Some retained API signatures use JetBrains type annotations that the server
    # does not need at runtime, but javac needs when resolving overloads.
    annotations = server / "annotations-26.1.0.jar"
    if not annotations.exists():
        cached = Path.home() / ".m2/repository/org/jetbrains/annotations/26.1.0/annotations-26.1.0.jar"
        if cached.is_file():
            shutil.copyfile(cached, annotations)
        else:
            with urllib.request.urlopen("https://repo.maven.apache.org/maven2/org/jetbrains/annotations/26.1.0/annotations-26.1.0.jar", timeout=60) as response, annotations.open("wb") as output:
                shutil.copyfileobj(response, output)
    dependencies.append(annotations)
    if hashlib.sha256(annotations.read_bytes()).hexdigest() != "ebc7aec252ed0c7d2d04c039d7f00e69f7b86b1f493c741d67b3ef31b986b054":
        raise RuntimeError("Pinned compile-time annotations checksum mismatch")
    javac = Path(java).with_name("javac")
    if not javac.is_file():
        javac = shutil.which("javac")
    if not javac or not dependencies:
        raise RuntimeError("Java 25 javac and prepared Paper libraries are required for the smoke probe")
    command = [str(javac), "--release", "25", "-encoding", "UTF-8", "-cp",
               os.pathsep.join(map(str, [plugin, *dependencies])), "-d", str(output)]
    command += list(map(str, (ROOT / "src/smoke/java").rglob("*.java")))
    subprocess.run(command, check=True)
    target = server / "plugins/MoarBowsSmoke.jar"
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as jar:
        for source in output.rglob("*.class"):
            jar.write(source, source.relative_to(output))
        jar.write(ROOT / "src/smoke/resources/plugin.yml", "plugin.yml")


def wait_for(process, log, predicate, timeout=120):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        content = log.read_text(errors="replace")
        if predicate(content):
            return content
        if process.poll() is not None:
            raise RuntimeError(f"Server exited before the expected result; see {log}")
        if "Unsupported API version" in content or "MOARBOWS_SMOKE_FAILURE" in content:
            raise RuntimeError(f"Plugin or smoke assertion failed; see {log}")
        time.sleep(0.25)
    raise RuntimeError(f"Timed out waiting for server; see {log}")


def run_phase(server, java, phase):
    log = server / f"smoke-{phase}.log"
    command = [java, "-Dterminal.jline=false", "-Dterminal.ansi=false", "-Xms512M", "-Xmx1536M",
               "-jar", "paper.jar", "--nogui"]
    with log.open("w") as output:
        process = subprocess.Popen(command, cwd=server, stdin=subprocess.PIPE, stdout=output,
                                   stderr=subprocess.STDOUT, text=True)

        def send(commands):
            process.stdin.write(commands + "\n")
            process.stdin.flush()

        try:
            wait_for(process, log, lambda text: 'For help, type "help"' in text
                     and "MoarBows ready: 28 bows, 28 recipes." in text)
            send("version\nplugins\nmb list\nmoarbowssmoke")
            wait_for(process, log, lambda text: text.count("MOARBOWS_SMOKE_RESULT") == 1)
            send("mb reload")
            wait_for(process, log, lambda text: text.count("MoarBows ready: 28 bows, 28 recipes.") == 2)
            send("mb list\nmoarbowssmoke")
            wait_for(process, log, lambda text: text.count("MOARBOWS_SMOKE_RESULT") == 2)
            send("stop")
            process.wait(timeout=45)
        finally:
            if process.poll() is None:
                try:
                    send("stop")
                    process.wait(timeout=30)
                except (BrokenPipeError, subprocess.TimeoutExpired):
                    process.terminate()
                    try:
                        process.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait()
            process.stdin.close()
    content = log.read_text(errors="replace")
    results = re.findall(r"MOARBOWS_SMOKE_RESULT assertions=(\d+) failures=(\d+) bows=(\d+)", content)
    listed = len(re.findall(r"INFO\]: \* ", content))
    problems = []
    if process.returncode != 0:
        problems.append(f"server exit code {process.returncode}")
    if len(results) != 2 or any(failures != "0" or bows != "28" for _, failures, bows in results):
        problems.append("missing or failed probe results")
    if listed != 56:
        problems.append(f"console listed {listed} bows, expected 28 before and after reload")
    if "[MoarBows] Enabling MoarBows" not in content or "[MoarBows] Disabling MoarBows" not in content:
        problems.append("plugin enable/disable was not confirmed")
    if "All RegionFile I/O tasks to complete" not in content or any(
            f"Halted I/O scheduler for world 'minecraft:{dimension}'" not in content
            for dimension in ("overworld", "the_nether", "the_end")):
        problems.append("world save during shutdown was not confirmed")
    if re.search(r"ERROR\]|Exception|Could not (?:register|reload)|FAILED TO BIND", content):
        problems.append("server log contains an error, exception, or plugin registration failure")
    if problems:
        raise RuntimeError("; ".join(problems) + f". See {log}")
    print(f"{server.name} {phase}: {sum(int(result[0]) for result in results)} assertions; "
          f"all 28 bows before/after reload; clean shutdown. {log}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--accept-eula", action="store_true", help="Accept https://aka.ms/MinecraftEULA for this test server")
    parser.add_argument("--version", choices=BUILDS, required=True)
    parser.add_argument("--jar", type=Path, required=True, help="Built production MoarBows JAR")
    parser.add_argument("--java", default="java", help="Java 25 executable; sibling javac is used for the probe")
    parser.add_argument("--server-dir", type=Path, help="Prepared isolated smoke server, never a live server")
    parser.add_argument("--server-jar", type=Path, help="Existing official server JAR, checksum verified")
    args = parser.parse_args()
    if not args.accept_eula:
        parser.error("Pass --accept-eula after accepting the Minecraft EULA")
    plugin = args.jar.resolve()
    if not plugin.is_file():
        parser.error("Production JAR does not exist")
    build, checksum, port = BUILDS[args.version]
    server = (args.server_dir or ROOT / f".scratch/runtime/paper-smoke-{args.version}-{build}").resolve()
    marker = server / ".moarbows-smoke-server"
    if server.exists() and not marker.exists():
        unexpected = set(p.name for p in server.iterdir()) - {"paper.jar", "libraries", "cache", "versions"}
        if unexpected:
            parser.error(f"Refusing to modify an unmarked server containing {sorted(unexpected)}")
    server.mkdir(parents=True, exist_ok=True)
    marker.touch()
    target = server / "paper.jar"
    if args.server_jar:
        if args.server_jar.resolve() != target:
            shutil.copyfile(args.server_jar, target)
    elif not target.exists():
        url = f"https://fill-data.papermc.io/v1/objects/{checksum}/paper-{args.version}-{build}.jar"
        request = urllib.request.Request(url, headers={"User-Agent": "MoarBows-Compatibility-Smoke/1.0"})
        with urllib.request.urlopen(request, timeout=60) as response, target.open("wb") as output:
            shutil.copyfileobj(response, output)
    if hashlib.sha256(target.read_bytes()).hexdigest() != checksum:
        parser.error("Pinned Paper server checksum mismatch")
    java = shutil.which(args.java) or args.java
    java_version = subprocess.check_output([java, "-version"], stderr=subprocess.STDOUT, text=True)
    if not re.search(r'version "25(?:\.|\")', java_version):
        parser.error("This compatibility smoke test requires Java 25")
    plugins = server / "plugins"
    plugins.mkdir(exist_ok=True)
    other_plugins = [p.name for p in plugins.glob("*.jar") if p.name not in {"MoarBows.jar", "MoarBowsSmoke.jar"}]
    if other_plugins:
        parser.error(f"Unrelated plugins are forbidden in this smoke server: {other_plugins}")
    shutil.copyfile(plugin, plugins / "MoarBows.jar")
    (plugins / "bStats").mkdir(exist_ok=True)
    (plugins / "bStats/config.yml").write_text("enabled: false\n")
    (server / "eula.txt").write_text("eula=true\n")
    (server / "server.properties").write_text(
        f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=true\n"
        "enable-rcon=false\nenable-query=false\nmax-players=1\nview-distance=2\n"
        "simulation-distance=2\nspawn-protection=0\nlevel-type=minecraft:flat\n"
        "generate-structures=false\n"
        'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},'
        '{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],'
        '"biome":"minecraft:plains"}\n')
    if not list((server / "libraries").rglob("paper-api-*.jar")):
        subprocess.run([java, "-Dpaperclip.patchonly=true", "-jar", "paper.jar"], cwd=server, check=True)
    build_probe(server, plugin, java)
    data = plugins / "MoarBows"
    legacy = {}
    config = data / "config.yml"
    if config.is_file() and not re.search(rb"(?m)^config-version:\s*[1-9]", config.read_bytes()):
        legacy = {name: (data / name).read_bytes() for name in ("config.yml", "bows.yml", "language.yml")}
    first_configuration = None
    for phase in ("first", "restart"):
        run_phase(server, java, phase)
        for name, original in legacy.items():
            backup = data / "backup-before-paper26" / name
            if not backup.is_file() or backup.read_bytes() != original:
                raise RuntimeError(f"Legacy migration did not preserve an exact backup of {name}")
        current = {name: (data / name).read_bytes() for name in ("config.yml", "bows.yml", "language.yml")}
        if first_configuration is not None and current != first_configuration:
            raise RuntimeError("Configuration changed across restart; migration is not idempotent")
        first_configuration = current
    if legacy:
        print("Legacy configuration: all three original backups match byte-for-byte; restart is idempotent.")
    print(f"Production SHA-256: {hashlib.sha256(plugin.read_bytes()).hexdigest()}")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        raise SystemExit(f"Build/bootstrap command failed with exit code {error.returncode}; see diagnostics above.") from None
    except RuntimeError as error:
        raise SystemExit(str(error)) from None
