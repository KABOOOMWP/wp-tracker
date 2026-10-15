#!/bin/sh
# Xcode build phase: fails on app.json drift, then builds Shared.framework for this configuration and SDK.
set -eu

python3 "$SRCROOT/../../tool/app_config.py" check

# The Gradle wrapper needs a JDK 17+: an installed one, else the runtime bundled with Android Studio
if [ -z "${JAVA_HOME:-}" ]; then
    if JAVA_HOME=$(/usr/libexec/java_home -v 17+ 2>/dev/null); then
        :
    else
        studio=$(mdfind "kMDItemCFBundleIdentifier == 'com.google.android.studio'" | head -n 1)
        JAVA_HOME="$studio/Contents/jbr/Contents/Home"
    fi
    if [ ! -x "$JAVA_HOME/bin/java" ]; then
        echo "error: no JDK 17+ found; install one or set JAVA_HOME" >&2
        exit 1
    fi
    export JAVA_HOME
fi

cd "$SRCROOT/.."
exec ./gradlew :shared:embedAndSignAppleFrameworkForXcode
