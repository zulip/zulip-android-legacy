#!/bin/bash
set -e

#Start the emulator
$ANDROID_HOME/tools/emulator -avd test -no-window -wipe-data &
EMULATOR_PID=$!

# Wait for Android to finish booting
WAIT_CMD="$ANDROID_HOME/platform-tools/adb wait-for-device shell getprop init.svc.bootanim"
until $WAIT_CMD | grep -m 1 stopped; do
  echo "Waiting..."
  sleep 1
done

# Unlock the Lock Screen
$ANDROID_HOME/platform-tools/adb shell input keyevent 82


# Run the tests
./gradlew connectedDebugAndroidTest -i

# save exit code of previous command
$exit_code = 0
if [$?]
then
    $exit_code = 0
else
    $exit_code = 1
fi

# Stop the background processes
kill $EMULATOR_PID

# fail tests when error occurs
exit $exit_code