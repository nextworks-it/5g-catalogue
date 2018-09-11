#!/bin/bash

curl -v -X DELETE -d @createNsdInfoId.json http://localhost:8083/nsd/v1/ns_descriptors/$1 --header "Accept:application/json"
