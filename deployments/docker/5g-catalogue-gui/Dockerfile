# Download base image
FROM httpd:2.4

# Update Ubuntu software repository
RUN apt update

# Install git
RUN apt install git -y

# Define variables
ARG catalogue_version=master
ARG catalogue_repo=https://github.com/nextworks-it/5g-catalogue.git
ARG catalogue_server_port=8083
ARG catalogue_profile=default
ARG catalogue_scope=public
ARG keycloak_enabled=true
ARG keycloak_url=http://localhost:8080/auth/
ARG keycloak_realm=default
ARG keycloak_gui_client=5gcatalogue

# Install 5G Apps and Services Catalogue GUI
WORKDIR /home
RUN git clone ${catalogue_repo} 5g-catalogue
WORKDIR /home/5g-catalogue
RUN git checkout ${catalogue_version}
RUN git submodule update --init

COPY templates/httpd.conf /usr/local/apache2/conf/
COPY logos /home/logos
RUN cp /home/logos/${catalogue_profile}_logo.png /home/5g-catalogue/5gcatalogue-gui/5gcatalogue/images/
COPY templates/side_top_bar_${catalogue_profile}.html /home/5g-catalogue/5gcatalogue-gui/5gcatalogue/templates/side_top_bar.html
RUN sed -i "s|_KEYCLOAK_URL_|${keycloak_url}|g" /home/5g-catalogue/5gcatalogue-gui/5gcatalogue/templates/side_top_bar.html \
    && sed -i "s|_KEYCLOAK_REALM_|${keycloak_realm}|g" /home/5g-catalogue/5gcatalogue-gui/5gcatalogue/templates/side_top_bar.html \
    && sed -i "s|_KEYCLOAK_GUI_CLIENT_|${keycloak_gui_client}|g" /home/5g-catalogue/5gcatalogue-gui/5gcatalogue/templates/side_top_bar.html
COPY templates/utils.js /home/5g-catalogue/5gcatalogue-gui/5gcatalogue/plugin/
RUN sed -i "s|_CATALOGUE_SERVER_PORT_|${catalogue_server_port}|g" /home/5g-catalogue/5gcatalogue-gui/5gcatalogue/plugin/utils.js \
    && sed -i "s|_CATALOGUE_SCOPE_|${catalogue_scope}|g" /home/5g-catalogue/5gcatalogue-gui/5gcatalogue/plugin/utils.js \
    && sed -i "s|_KEYCLOAK_ENABLED_|${keycloak_enabled}|g" /home/5g-catalogue/5gcatalogue-gui/5gcatalogue/plugin/utils.js

RUN apachectl restart