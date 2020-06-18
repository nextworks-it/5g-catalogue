#!/bin/bash

mkdir -p ~/.docker/
touch ~/.docker/config.json
echo "{\"proxies\":{\"default\":{\"httpProxy\":\"http://163.162.8.137:9777\",\"httpsProxy\":\"http://163.162.8.137:9777\",\"noProxy\":\"172.17.254.0/24,172.17.105.0/24,10.50.7.0/24\"}}}" > ~/.docker/config.json

docker-compose -f "docker-compose.yml" up -d --build