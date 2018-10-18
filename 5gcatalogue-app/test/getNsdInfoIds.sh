#!/bin/bash

curl -v -X GET http://localhost:8083/nsd/v1/ns_descriptors --header "Accept:application/json"
