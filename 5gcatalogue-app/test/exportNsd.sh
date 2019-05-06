#!/bin/bash

curl -X POST -v http://localhost:8083/catalogue/cat2catOperation/exportNsd/$1 --header "Accept:application/json"
