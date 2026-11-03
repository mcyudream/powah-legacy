#!/usr/bin/env bash
set -euo pipefail
powah_project=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
powah_jdk="$powah_project/../../.dev/jdk-25.0.5+8"
if [[ -n "${POWAH_JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$POWAH_JAVA_HOME"
elif [[ -x "$powah_jdk/bin/java" ]]; then
    export JAVA_HOME="$powah_jdk"
fi
powah_proxy_args=()
powah_proxy="${HTTPS_PROXY:-${https_proxy:-}}"
if [[ "$powah_proxy" =~ ^https?://([^/:@]+):([0-9]+)/?$ ]]; then
    powah_proxy_args+=("-Dhttps.proxyHost=${BASH_REMATCH[1]}" "-Dhttps.proxyPort=${BASH_REMATCH[2]}")
    powah_proxy_args+=("-Dhttp.proxyHost=${BASH_REMATCH[1]}" "-Dhttp.proxyPort=${BASH_REMATCH[2]}")
fi
cd -- "$powah_project"
powah_tasks=()
for powah_arg in "$@"; do
    case "$powah_arg" in
        runClient|runServer|build|test|clean|devHarnessJar) powah_tasks+=(":$powah_arg") ;;
        *) powah_tasks+=("$powah_arg") ;;
    esac
done
exec ./gradlew "${powah_proxy_args[@]}" "${powah_tasks[@]}"
