#!/usr/bin/env bash

docker run --rm -it spotify/kafka /opt/kafka_2.11-0.10.1.0/bin/kafka-console-consumer.sh --bootstrap-server `ipconfig getifaddr en0`:9092 --from-beginning --topic mqtttest
