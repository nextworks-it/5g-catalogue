#!/bin/bash


git clone https://github.com/swagger-api/swagger-codegen.git

cd swagger-codegen
mvn clean package

java -jar ./modules/swagger-codegen-cli/target/swagger-codegen-cli.jar \
     generate -i ../interfaces/sol005/NSDManagement.yaml \
     -l spring \
     -o ../apigen/sol005nsdmanagement

cd apigen/sol005nsdmanagement
mvn clean package
mvn spring-boot:run
