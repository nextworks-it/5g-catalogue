#!/bin/bash

curl -v -X PATCH -d @Jsons/updateNsdInfoId.json http://localhost:8083/nsd/v1/ns_descriptors/$1 --header "Content-Type:application/json" --header "Accept:application/json"
