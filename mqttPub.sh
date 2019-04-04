#!/usr/bin/env bash
export MESSAGE=$1

if [[ -z "$MESSAGE" ]]; then
  MESSAGE="{\"id\":1234,\"message\":\"This is a test\"}"}
fi

echo "publishing $MESSAGE"
docker run -it --rm --name mqtt-publisher efrecon/mqtt-client pub -h `ipconfig getifaddr en0`  -t "mqtttest" -m "$MESSAGE"
