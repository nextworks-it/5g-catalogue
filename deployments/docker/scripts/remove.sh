#!/bin/bash

if [ -z "$1" ]
then
   echo -e "Please provide a valid docker-compose file.\nUsage example: '$0 docker-compose.yml'";
   exit 1
fi

docker-compose -f "$1" down --volumes --rmi all
sudo rm -rf /opt/5gcatalogue-app