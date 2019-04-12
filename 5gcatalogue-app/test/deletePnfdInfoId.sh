#!/bin/bash

curl -v -X DELETE -d @Jsons/createPnfdInfoId.json http://localhost:8083/nsd/v1/pnf_descriptors/$1 --header "Accept:application/json"
