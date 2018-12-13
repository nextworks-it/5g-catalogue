#!/bin/bash

curl -v -X POST -d @Jsons/createVnfPkgInfoId.json http://localhost:8083/vnfpkgm/v1/vnf_packages --header "Content-Type:application/json" --header "Accept:application/json"
