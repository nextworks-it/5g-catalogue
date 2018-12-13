#!/bin/bash

curl -v -X PATCH -d @Jsons/updateVnfPkgInfoId.json http://localhost:8083/vnfpkgm/v1/vnf_packages/$1 --header "Content-Type:application/json" --header "Accept:application/json"
