#!/bin/bash

curl -X PUT -v -F file=@$2 http://localhost:8083/nsd/v1/ns_descriptors/$1/nsd_content/?project=admin --header "Accept:application/json"
