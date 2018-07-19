#!/bin/bash

BANNER_H="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
BANNER_S="\n\n\n\n\n\n"

function log()
{
    echo $BANNER_H
    echo $1
    echo -e $BANNER_S
}

function myhelp ()
{
    echo "Usage: $0 <CMD>"
    echo "-----"
    echo " CMD := gen (generate) | cmp (compile) | run | all"
}

function generateAPIs()
{
    log "RETRIEVING TOOLS FOR API GENERATIONGENERATE SOL005 APIs"

    if [ ! -d  swagger-codegen ]; then
	log "CLONING swagger-codegen"
	git clone https://github.com/swagger-api/swagger-codegen.git
    fi

    cd swagger-codegen
    mvn clean package


    log " GENERATING SOL005 APIs"
    java -jar ./modules/swagger-codegen-cli/target/swagger-codegen-cli.jar \
	 generate -i ../interfaces/sol005/NSDManagement.yaml \
	 -l spring \
	 -o ../apigen/sol005nsdmanagement
    cd ..
}

function compileAPIs()
{
    log "GENERATING SOL005 APIs"
    cd apigen/sol005nsdmanagement
    mvn clean package
    cd ../../
}

function runAPIs()
{
    log "RUNNING SOL005 APIs AS A MODULE"
    cd apigen/sol005nsdmanagement
    mvn spring-boot:run 
    cd ../../
}


case $1 in
    (gen | generate)
	generateAPIs
	;;
    (cmp | compile)
	compileAPIs
	;;
    (run)
	runAPIs
	;;
    (all)
	generateAPIs;
	compileAPIs;
	runAPIs;
    ;;
    (*)
	echo "Invalid option '$1'!!!!"
	myhelp;
	exit -1
	;;
esac
