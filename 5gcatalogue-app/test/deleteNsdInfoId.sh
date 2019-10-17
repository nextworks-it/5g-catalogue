#!/bin/bash

curl -v -X DELETE -d @Jsons/createNsdInfoId.json http://localhost:8083/nsd/v1/ns_descriptors/$1/?project=admin --header "Accept:application/json"
