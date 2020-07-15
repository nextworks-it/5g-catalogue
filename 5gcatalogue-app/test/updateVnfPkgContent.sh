#!/bin/bash

curl -X PATCH -v -F file=@$2 http://localhost:8083/vnfpkgm/v1/vnf_packages/$1/package_content/?project=admin --header "Accept:application/json"
