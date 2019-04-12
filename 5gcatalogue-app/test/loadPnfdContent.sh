#!/bin/bash

curl -X PUT -v -F file=@$2 http://localhost:8083/nsd/v1/pnf_descriptors/$1/pnfd_content --header "Accept:application/json"
