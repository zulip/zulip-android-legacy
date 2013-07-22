#!/bin/bash
# Downloads docs and sources for third party libraries
set -e

if [[ `ls` != *.jar* ]]; then
    echo Run this script fron libs/! >&2
    exit 1
fi

ORMLITE_VER=4.45
ANDROID_SUPPORT_VER=v13

wget -nv -N http://ormlite.com/releases/$ORMLITE_VER/ormlite-core-$ORMLITE_VER-sources.jar
wget -nv -N http://ormlite.com/releases/$ORMLITE_VER/ormlite-core-$ORMLITE_VER-javadoc.jar
wget -nv -N http://ormlite.com/releases/$ORMLITE_VER/ormlite-android-$ORMLITE_VER-sources.jar
wget -nv -N http://ormlite.com/releases/$ORMLITE_VER/ormlite-android-$ORMLITE_VER-javadoc.jar

echo Sources and javadocs downloaded successfully to libs/
echo

ANDROID_PATH=$(dirname $(dirname $(which android)))

if [ -z "$ANDROID_PATH" ]; then
    echo You do not appear to have the Android tools in your path! >2&
    exit 2
fi

ASD_DIR=$ANDROID_PATH/extras/android/support/$ANDROID_SUPPORT_VER/src/

if [ ! -d "$ASD_DIR" ]; then
    echo You do not seem to have $ANDROID_SUPPORT_VER of the Android \
        Support library installed, or the docs are not at the expected \
        location: >&2
    echo -e "\t$ASD_DIR" >&2
    exit 3
fi

if [ -e android-support-$ANDROID_SUPPORT_VER-sources ]; then
    exit 0
fi

ln -sT $ASD_DIR android-support-$ANDROID_SUPPORT_VER-sources

exit 0
