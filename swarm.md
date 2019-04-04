# Instructions on deploying in a swarm
see [here](https://docs.docker.com/get-started/part3/)

## Ensure we're in a swarm, e.g.:

 * Init/connect to a swarm
 
```bash
docker swarm init
```

 * Deploy the app
 ```bash
docker stack deploy -c ./mqtt/src/main/docker/docker-compose.yml mqtt4kafka-swarm
```

 * Check it's running:
 ```bash
docker service ls
```
or
 ```bash
docker stack services mqtt4kafka-swarm
 ```
 
 * See the processes in the stack
 ```bash
docker stack ps mqtt4kafka-swarm
 ```
 
 * Take down the stack:
 ```bash
 docker stack rm mqtt4kafka-swarm
  ```
