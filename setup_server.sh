#!/usr/bin/env bash
set -e

git clone https://github.com/zulip/zulip.git
cd zulip

# From a clone of zulip.git
./tools/travis/setup-backend
tools/clean-venv-cache --travis
cd ..