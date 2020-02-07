#!/bin/bash

sudo docker-compose -f "docker-compose.yml" down --volumes --rmi all
sudo docker-compose -f "docker-compose.yml" up -d --build