#!/bin/sh

set -e

./watch_replay.sh `ls replays/replay-* | sort -r | head -n 1`