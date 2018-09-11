#!/bin/bash

curl -v -X PUT --data-binary @vCDN_tosca_v02.yaml http://localhost:8083/nsd/v1/ns_descriptors/$1/nsd_content --header "Content-Type:application/x-yaml" --header "Accept:application/json"

curl -v -X GET http://localhost:8083/nsd/v1/ns_descriptors/$1/nsd_content --header "Content-Type:application/x-yaml" --header "Accept:application/x-yaml"
