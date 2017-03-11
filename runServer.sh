#!/usr/bin/env bash
set -e

# starts the development server
cd zulip
source tools/travis/activate-venv
./tools/run-dev.py