#!/usr/bin/env bash
set -e

# install sdk
curl --location http://dl.google.com/android/android-sdk_r24.4.1-linux.tgz | tar -x -z -C $HOME
export ANDROID_HOME=$HOME/android-sdk-linux
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# update sdk with build tools and other components used by the project
( sleep 5 && while [ 1 ]; do sleep 1; echo y; done ) | android update sdk -u -a -t 1,2,11,34,64,166,172,173
