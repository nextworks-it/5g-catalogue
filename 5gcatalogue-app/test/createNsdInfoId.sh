#!/bin/bash

curl -v -X POST -d @Jsons/createNsdInfoId.json http://localhost:8083/nsd/v1/ns_descriptors --header "Content-Type:application/json" --header "Accept:application/json"
