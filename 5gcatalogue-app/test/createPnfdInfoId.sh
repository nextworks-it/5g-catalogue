#!/bin/bash

curl -v -X POST -d @Jsons/createPnfdInfoId.json http://localhost:8083/nsd/v1/pnf_descriptors --header "Content-Type:application/json" --header "Accept:application/json"
