#!/bin/bash

curl -v -X GET http://localhost:8083/vnfpkgm/v1/vnf_packages/$1/package_content --header "Accept:application/zip"
