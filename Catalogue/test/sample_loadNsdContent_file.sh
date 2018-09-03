#!/bin/bash

curl -X PUT -v -F file=@vCDN_tosca_v01.yaml http://localhost:8083/nsd/v1/ns_descriptors/XXXXXXX/nsd_content --header "Accept:application/json"
#curl -v -X PUT --data-binary @vCDN_tosca_v01.yaml http://localhost:8083/nsd/v1/ns_descriptors/XXXXXXX/nsd_content --header "Content-Type:application/x-yaml" --header "Accept:application/json"

#curl -v -X GET http://localhost:8083/nsd/v1/ns_descriptors/XXXXXXX/nsd_content --header "Content-Type:application/x-yaml" --header "Accept:application/x-yaml"
