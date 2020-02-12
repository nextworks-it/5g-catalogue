#!/bin/bash

docker-compose -f "docker-compose.yml" down --volumes --rmi all
sudo rm -rf volumes/catalogue-app volumes/postgres