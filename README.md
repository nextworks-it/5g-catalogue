# 5G Apps & Services Catalogue

NFV-SDN Catalogue capable of storing and  versioning:

- Network Service Descriptors (NSDs)
- Virtual Network Function Packages (VNF Packages)

#### In roadmap
- Software Defined Network App Packages (SDN App Packages)
- Multi-access Edge Computing App Packages (MEC App Packages)
- Physical Network Function Descriptors (PNFs)

## Getting Started

### Prerequisites

* [Oracle-Java8] - Oracle version 8 is preferred, otherwise [OpenJDK-8] + [OpenJFX]
* [Maven] - version >= 3.3.9 is required
* [PostgresSQL] - as internal DB 
* [Apache Kafka] - as internal message bus, configured with the following topics: catalogue-onboarding-local, catalogue-onboarding-remote

### Used Libraries

| Lib | REPOSITORY |
| ------ | ------ |
| NfvManoLibsSol001Common | [README](https://github.com/nextworks-it/nfv-sol-libs) |
| NfvManoLibsSol001Descriptors | [README](https://github.com/nextworks-it/nfv-sol-libs) |
| J-OSMClient | [README](https://github.com/girtel/J-OSMClient) |
| OpenStack4j | [README](https://github.com/ContainX/openstack4j) |

### Installing

Run the "bootstrap" script for setting up the environment:

- Create the Catalogue log folder, "/var/log/5gcatalogue"
- Install PostgresSQL (if not present) and create the Catalogue DB, "cataloguedb"
- Clone and install "NfvManoLibsSol001Common" and "NfvManoLibsSol001Descriptors" libs

```
$ ./bootstrap.sh env-dep
```
### Compiling

Compile the 5G Apps & Services Catalogue application:

```
$ ./bootstrap.sh compile-app
```

### Running

Run the 5G Apps & Services Catalogue application:

```
$ ./bootstrap.sh run-app
```

#### NOTE

For installing, compiling and executing in sequence:

```
$ ./bootstrap.sh all
```

## Versioning

For the versions available, see tags on this repository. 

## Authors

* **Francesca Moscatelli**, **Giacomo Bernini**, **Giada Landi**, **Marco Capitani**, **Leonardo Agueci** - [Nextworks S.r.l.](http://www.nextworks.it)

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details

