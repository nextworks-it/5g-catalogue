#!/bin/bash

BANNER_H="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
BANNER_S="\n\n\n\n\n\n"
GITUSER=$USER

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
    echo " CMD := generate-api | compile-api | run-api | all-api |"
    echo "        env-dep"
}

function prepareEnv()
{
    log "PREPARING ENVIRONMENT"

    if [ ! -d  /var/log/5gcatalogue/ ]; then
        sudo mkdir  /var/log/5gcatalogue/
        sudo chmod a+rw /var/log/5gcatalogue/
    fi

    which psql
    if [ "$?" -gt "0" ]; then
        log "POSTGRES not installed... Installing"
        sudo apt-get install postgresql -y
    fi
    sudo -u postgres createdb cataloguedb

    log "BUILDING NFV-LIB DEPENDENCIES..."
    if [ ! -d  ../nfv-libs/ ]; then
        cd ..
        git clone ssh://${GITUSER}@terrance.nextworks.it/git/KD/nfv-libs
        cd 5g-catalogue
    fi

    cd ../nfv-libs/NFV_MANO_LIBS_COMMON
    mvn clean install
    cd ../NFV_MANO_LIBS_DESCRIPTORS
    mvn clean install
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
    (generate-api)
        generateAPIs
        ;;
    (compile-api)
        compileAPIs
        ;;
    (run-api)
        runAPIs
        ;;
    (all-api)
        generateAPIs;
        compileAPIs;
        runAPIs;
        ;;
    (env-dep)
        prepareEnv
        ;;
    (*)
        echo "Invalid option '$1'!!!!"
        myhelp;
        exit -1
        ;;
esac
