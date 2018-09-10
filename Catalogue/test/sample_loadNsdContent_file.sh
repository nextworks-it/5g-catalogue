#!/bin/bash

curl -X PUT -v -F file=@vCDN_tosca_v02.yaml http://localhost:8083/nsd/v1/ns_descriptors/58c25769-567d-4e68-87d7-e13c28fb1a84/nsd_content --header "Accept:application/json"
