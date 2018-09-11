#!/bin/bash

curl -X PUT -v -F file=@two_cirros_example_tosca.yaml http://localhost:8083/nsd/v1/ns_descriptors/$1/nsd_content --header "Accept:application/json"
