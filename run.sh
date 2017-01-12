#!/usr/bin/env bash
set -e

# enable auth backends
sed -i "19s/.*/AUTHENTICATION_BACKENDS = ('zproject.backends.EmailAuthBackend',)/" zulip/zproject/dev_settings.py

# run server and emulator in parallel
./runServer.sh &
./runTests.sh
wait