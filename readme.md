# MQTT Connect 

A project which combines an MQTT client with a Kafka Publisher.

The container starts both a mosquitto MQTT broker and our MQTT client -- a poor-man's means of exposing a process which maps MQTT messagse to Kafka
 


See [here](https://github.com/wurstmeister/kafka-docker/wiki/Connectivity) and [here](https://github.com/simplesteph/kafka-stack-docker-compose) for docker compose w/ kafka 

## Building 

create a docker image which starts an MQTT broker service which publishes messages to Kafka called "mqtt4kafka:latest":

```bash
sbt docker
``` 

If you want to just run the Main class, you can start an MQTT broker and Kafka via:
```bash
./mqttBroker.sh
./kafkaStart.sh
```

When building, the tests will bring up/tear down their own brokers/kafka images

## Running 

Bring up an environment which includes our container with kafka/ZK:
```bash
./up.sh
```

### Test Connectivity

Publish an MQTT message:
```bash
./mqttPub.sh helloWorld
```

Observe the message(s) in kafka:
```bash
./kafkaSub.sh
```

# TODO
 1) update pipeline yml to run containers in the pipeline tests
 -) end-to-end integration test of MQTT messages in observed from consuming a kafka topic
 3) deployment: mapping the bytes to our/a schema
 4) ensure the consumers read from the latest (ish) feeds
 5) provide a high-available broker
 6) load test/soak test
 7) check the HA requirements for the client


### Kafka
This piece shows how the MQTT stream from 'mqtt-connect' can be linked together with a kafka stream:


MQTT client --> MQTT broker --> Kafka Client --> profit!

The basic kafka input/output connectivity is demonstrated by connecting to a kafka instance started in a container.

This way we can perform more meaningful testing. 

## The Kafka container

The src/test/resources/scripts contain scripts for starting, stopping and checking if kafka is running.

Those scripts are invoked programatically via 'KafkaEnv' (which in turn is called from BaseSpec, which is a base class for tests 
to ensure kafka is running before/after all the tests are run).

In the build pipeline, our bitbucket yaml can be instructed to run the same container, so the starting/stopping part will only
really be relevant for local development.

### Manually checking

#### Starting Kafka
To manually demonstrate that we can insert/read data from kafka, you could run the
```bash
src/test/resources/scripts/startKafkaDocker.sh
```

or 

```bash
docker-compose -f ./mqtt/src/main/docker/compose/zk-single-kafka-single.yml up

```

#### Pushing data to Kafka
and then push data from e.g. stdin by running:
```bash
docker run --rm --name kafkaproducer --env HOST_IP=`ipconfig getifaddr en0` -it spotify/kafka /bin/bash
> /opt/kafka_2.11-0.10.1.0/bin/kafka-console-producer.sh --broker-list ${HOST_IP}:9092 --topic mqtttest
```

#### Consuming data from Kafka
and view/consume the data from the 'test' topic via
```bash
docker run --rm -it spotify/kafka /opt/kafka_2.11-0.10.1.0/bin/kafka-console-consumer.sh --bootstrap-server `ipconfig getifaddr en0`:9092 --from-beginning --topic test
```

or using the console consumer:
```bash
docker run --rm --env HOST_IP=`ipconfig getifaddr en0` -it spotify/kafka /bin/bash
> /opt/kafka_2.11-0.10.1.0/bin/kafka-console-consumer.sh --bootstrap-server ${HOST_IP}:9092 --from-beginning --topic test
```


### Basic Checking

Given the manual checks work, we can also push/consume data using the 'BasicConsumer' and 'BasicProducer' apps under the test
sources.

We can then demonstrate the data in/out from a host environment via a container (which isn't necessarily trivial)

### Alpakka Akka Kafka Checking

Now that the basic building blocks are in place, we can prove connectivity via our alpakka.
This is the main point of the automated test - that our KafkaIngress works via streaming.

Should we need to tweak or otherwise configure kafka, we can revisit the more basic steps as above. 


# Other MQTT offerings


### Lenses 'dataops'
https://docs.lenses.io/connectors/source/mqtt.html
https://www.landoop.com/lenses-overview/

