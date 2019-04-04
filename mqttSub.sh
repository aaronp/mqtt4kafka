#!/usr/bin/env bash

docker run --init -it --rm efrecon/mqtt-client sub  -h `ipconfig getifaddr en0` -t "#" -v