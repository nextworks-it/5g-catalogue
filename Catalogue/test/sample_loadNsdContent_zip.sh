#!/bin/bash

curl -X POST -v -F file=@vnf-test.tar http://localhost:8081/app-catalogue/app-packages
#curl -v -X PUT --data-binary @vCDN_tosca_v01.yaml http://localhost:8083/nsd/v1/ns_descriptors/XXXXXXX/nsd_content --header "Content-Type:application/x-yaml" --header "Accept:application/json"

curl -v -X GET http://localhost:8083/nsd/v1/ns_descriptors/XXXXXXX/nsd_content --header "Content-Type:application/x-yaml" --header "Accept:application/x-yaml"
