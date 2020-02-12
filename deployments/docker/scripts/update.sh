#!/bin/bash

docker-compose -f "docker-compose.yml" down --volumes --rmi all
docker-compose -f "docker-compose.yml" up -d --build