# 5G Apps & Services Catalogue

## Getting Started

### Prerequisites

For running properly the webgui, submodules are required:

```
$ git submodule update --init
``` 

### Installing

Apache 2 configuration for running the WEB GUI (Ubuntu)

```
$ sudo apt-get update
$ sudo apt-get install apache2

$ cd /etc/apache2/
```

Edit apache2.conf adding the following lines:

```
<Directory <path_to_web_gui_repo>/catalogue-webgui>
        Options Indexes FollowSymLinks Includes
        AllowOverride None
        Require all granted
</Directory>
```

- NOTE: the path must point at the directory that includes also the WEB GUI template "gentelella", i.e. "catalogue-webui"

```
$ cd /etc/apache2/sites-enabled/
```

Edit 000-default.conf:

```
DocumentRoot <path_to_web_gui_repo>/catalogue-webgui
```

```
$ cd /etc/apache2/conf-available/
```

Create ssi.conf with the following lines:

```
AddType text/html .html
AddOutputFilter INCLUDES .html
```

```
$ cd /etc/apache2/conf-enabled/
```

Create link to ../conf-available/ssi.conf

```
$ sudo ln -sv ../conf-availabe/ssi.conf ssi.conf

$ cd /etc/apache2/mods-enabled/
```

Create link to ../mods-available/include.load

```
$ sudo ln -sv ../mods-available/include.load include.load

$ sudo service apache2 restart
$ sudo service apache2 status
```

## WEB GUI at http://localhost
