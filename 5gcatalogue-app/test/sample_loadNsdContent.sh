#!/bin/bash

curl -v -X PUT --data-binary @$2 http://localhost:8083/nsd/v1/ns_descriptors/$1/nsd_content --header "Content-Type:application/x-yaml" --header "Accept:application/json"

curl -v -X GET http://localhost:8083/nsd/v1/ns_descriptors/$1/nsd_content --header "Content-Type:application/x-yaml" --header "Accept:application/x-yaml"
