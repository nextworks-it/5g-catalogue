#!/bin/bash

mkdir -p ~/.docker/
touch ~/.docker/config.json
echo "{\"proxies\":{\"default\":{\"httpProxy\":\"http://163.162.8.137:9777\",\"httpsProxy\":\"http://163.162.8.137:9777\"}}}" > ~/.docker/config.json

docker-compose -f "docker-compose.yml" up -d --build