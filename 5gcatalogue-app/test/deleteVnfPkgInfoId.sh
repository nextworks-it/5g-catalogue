#!/bin/bash

curl -v -X DELETE http://localhost:8083/vnfpkgm/v1/vnf_packages/$1/?project=admin --header "Accept:application/json"
