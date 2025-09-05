#!/usr/bin/env bash
set -euo pipefail

# 1) Disable any proxies that may break TLS to Gradle mirrors
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY no_proxy NO_PROXY JAVA_TOOL_OPTIONS || true
export JAVA_TOOL_OPTIONS=""
export GRADLE_OPTS="${GRADLE_OPTS:-} -Djava.net.useSystemProxies=false"

# 2) Prefer local Gradle zip if provided in repo to avoid any external network
LOCAL_ZIP="ci/gradle/gradle-8.13-bin.zip"
if [ -f "$LOCAL_ZIP" ]; then
  ABS_ZIP="file://$(cd "$(dirname "$LOCAL_ZIP")" && pwd)/$(basename "$LOCAL_ZIP")"
  sed -i "s#^distributionUrl=.*#distributionUrl=${ABS_ZIP//#/\#}#" gradle/wrapper/gradle-wrapper.properties
else
  # If provided, download from custom URL (e.g., your OSS/GitHub Release)
  if [ -n "${GRADLE_ZIP_URL:-}" ]; then
    mkdir -p ci/gradle
    echo "Downloading Gradle from $GRADLE_ZIP_URL ..."
    curl -L --retry 3 --connect-timeout 20 -o "$LOCAL_ZIP" "$GRADLE_ZIP_URL"
    ABS_ZIP="file://$(cd "$(dirname "$LOCAL_ZIP")" && pwd)/$(basename "$LOCAL_ZIP")"
    sed -i "s#^distributionUrl=.*#distributionUrl=${ABS_ZIP//#/\#}#" gradle/wrapper/gradle-wrapper.properties
  else
  # Fallback to domestic mirror (idempotent replacement)
  sed -i 's#https://services.gradle.org/distributions/#https://mirrors.cloud.tencent.com/gradle/distributions/#g' gradle/wrapper/gradle-wrapper.properties || true
  sed -i 's#https://downloads.gradle-dn.com/distributions/#https://mirrors.cloud.tencent.com/gradle/distributions/#g' gradle/wrapper/gradle-wrapper.properties || true
  fi
fi
echo "Using distributionUrl:"
grep '^distributionUrl=' gradle/wrapper/gradle-wrapper.properties || true

# 3) Build
chmod +x ./gradlew || true
./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -Dfile.encoding=UTF-8" clean assembleRelease

echo "Build finished. APK should be at app/build/outputs/apk/release/app-release.apk"


