# Autonomous JSON Database Demo

## M1/M2 Pre-steps

Use brew and software update to install all the required emulation (probably already have docker and docker-compose).

```shell
brew install docker
brew install docker-compose
brew install colima

softwareupdate --install-rosetta
```

Start the compatible VM with x86_64 using

```shell
colima start --cpu 4 --memory 16 --arch x86_64 --vm-type vz --vz-rosetta
```

Now move on to normal local set up.

## Setup Local DB

First install the latest image

```shell
docker pull container-registry.oracle.com/database/adb-free:latest
```

Then you can start a container instance:

```shell
docker run -d \
-p 1521:1522 \
-p 1522:1522 \
-p 8443:8443 \
-p 27017:27017 \
-e WORKLOAD_TYPE='ATP' \
-e WALLET_PASSWORD=Longpassword1 \
-e ADMIN_PASSWORD=Longpassword1 \
--hostname localhost \
--cap-add SYS_ADMIN \
--device /dev/fuse \
--name adb_container \
container-registry.oracle.com/database/adb-free:latest
```

Note the container ID returned by the above command, or look it up using `docker ps`, for the following commands.

In order for this demo to run, we either need to setup our Wallet or we need to update our Java keystore. If you are
using a wallet, everything is already configured to run locally. If you are not using a wallet, you need to install it
in your Java keystore (the first command will cleanup any prior instances):

```shell
docker cp <CONTAINER_ID>:u01/app/oracle/wallets/tls_wallet/adb_container.cert /tmp/tls_wallet/adb_container.cert

keytool -delete -alias adb_container_certificate -cacerts
keytool -import -alias adb_container_certificate -cacerts -file /tmp/tls_wallet/adb_container.cert
```

## Demo

You can see the code demo [here](src/main/kotlin/com/projectronin/interop/soda/SodaDemo.kt).

The Docker image also starts up an instance
of [Oracle's Database Actions](https://localhost:8443/ords/admin/_sdw/) that you can use to access the JSON and
SQL views through your browser much like you would in OCI. Use the admin username and the password you set earlier.
