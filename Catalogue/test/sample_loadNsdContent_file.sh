#!/bin/bash

curl -X PUT -v -F file=@vCDN_tosca_v02.yaml http://localhost:8083/nsd/v1/ns_descriptors/$1/nsd_content --header "Accept:application/json"
