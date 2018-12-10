#!/bin/sh

set -e
./gradlew build

java -jar simulator/build/libs/simulator.jar