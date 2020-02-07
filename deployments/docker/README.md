# 5G Apps & Services Catalogue Docker Deployment

## Getting Started

### Prerequisites

* [Docker Engine] - Docker Engine Community version 17.06.0+
* [Docker Compose] - Docker Compose version 1.21.0+

### Configuration

For properly configuring the 5G Apps & Services Catalogue, the [.env](https://github.com/nextworks-it/5g-catalogue/blob/master/deployments/docker/.env) file has to be modified according to the environment where the catalogue is deployed.

| Parameter | Description |
| ------ | ------ |
| CATALOGUE_VERSION | 5G Apps & Services Catalogue version |
| CATALOGUE_SOL_LIBS_VERSION | NfvManoLibsSol001 version |
| CATALOGUE_SERVER_PORT | 5G Apps & Services Catalogue Server port |
| CATALOGUE_GUI_PORT | 5G Apps & Services Catalogue GUI port |
| CATALOGUE_PROFILE | Four different profiles are defined. See below for more information |
| CATALOGUE_SCOPE | Scope can be "PUBLIC" or "PRIVATE". If "PRIVATE", user must provide ID and URL of a Public 5G Apps & Services Catalogue |
| PUBLIC_CATALOGUE_ID | Identifier of the Public 5G Apps & Services Catalogue |
| PUBLIC_CATALOGUE_URL | URL of the Public 5G Apps & Services Catalogue |
| MANO_ID | Identifier of the MANO |
| MANO_TYPE | Three different MANOs are currently supported: OSMR4, OSMR5, OSMR6 |
| MANO_SITE | Identifier of the site to which the MANO belongs |
| MANO_IP | IP address of the MANO |
| MANO_USERNAME | MANO username |
| MANO_PASSWORD | MANO password |
| MANO_PROJECT | MANO project |
| KEYCLOAK_AUTHENTICATION | Enable/Disable Keycloak authentication. If "true", following parameters must be set correctly |
| KEYCLOAK_URL | URL of the Keycloak server |
| KEYCLOAK_REALM | Keycloack realm |
| KEYCLOAK_BE_CLIENT | 5G Apps & Services Catalogue Server client configured on Keycloak |
| KEYCLOAK_BE_PUBLIC_CLIENT | Identifies if the 5G Apps & Services Catalogue Server client on Keycloack is configured with public access type or not |
| KEYCLOAK_BE_CLIENT_SECRET | If the 5G Apps & Services Catalogue Server client is not "PUBLIC", client secret must be specified |
| KEYCLOAK_USER_GROUP | Keycloak User Group where 5G Apps & Services Catalogue users will be added |
| KEYCLOAK_ADMIN_ROLE | Keycloak User Role with administrative privileges |
| KEYCLOAK_USER_ROLE | Keycloak User Role with limited privileges |
| KEYCLOAK_GUI_CLIENT | 5G Apps & Services Catalogue GUI client configured on Keycloak. It must be configured with public access type |

Inside the folder [5g-catalogue-app/profiles](https://github.com/nextworks-it/5g-catalogue/blob/master/deployments/docker/5g-catalogue-app/profiles/) four different profiles are defined: 

* "5gcity", "5gmedia" and "5geve" profiles ready to be used for deploying the catalogue
* The "default" profile, instead, must be modified before being used according to the environment where the catalogue is deployed  

| Parameter | Description |
| ------ | ------ |
| CATALOGUE_DEFAULT_PROJECT | Default project to use when not specified in the request |
| CATALOGUE_DB_MODE | Database mode: "update" or "create-drop" |
| CATALOGUE_STARTUP_SYNC | Enable/Disable startup synchronization from MANO |
| CATALOGUE_RUNTIME_SYNC | Enable/Disable runtime synchronization from MANO |
| CATALOGUE_RUNTIME_SYNC_PERIOD | Runtime synchronization period |
| CATALOGUE_MANO_TYPE | Two different MANO types are currently supported: DUMMY, OSM |
| CATALOGUE_SKIP_MANO_CONFIG | Skip MANO plugin configuration |
| CATALOGUE_OSM_VIM_NETWORK_NAME | Enable/Disable parameter "vim-network-name" on OSM NS descriptors |
| CATALOGUE_LOGO_PATH | Directory where is possible to put the logo to be associated with the translated descriptors |
| CATALOGUE_VIM_TYPE | Under development |
| CATALOGUE_SKIP_VIM_CONFIG | Under development |
| CATALOGUE_SKIP_VIM_CONFIG | Under development |


### Build and Run Containers

```
$ sudo docker-compose -f "docker-compose.yml" up -d --build
```
