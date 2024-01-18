# Autonomous JSON Database Demo

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

In order for this demo to run, we either need to setup our Wallet or we need to update our Java keystore.

### Wallet

In order for the wallet approach to work, we need to export the wallet from the container.

`docker cp adb_container:/u01/app/oracle/wallets/tls_wallet /tmp/tls_wallet`

### Non-Wallet

You need to extract the self-signed certificate from the container:
`docker cp adb_container:/u01/app/oracle/wallets/tls_wallet/adb_container.cert adb_container.cert`

Then you need to install it in your Java keystore (the first command will cleanup any prior instances):

```shell
sudo keytool -delete -alias adb_container_certificate -keystore $JAVA_HOME/lib/security/cacerts
sudo keytool -import -alias adb_container_certificate -keystore $JAVA_HOME/lib/security/cacerts -file adb_container.cert
```

## Demo

You can see the code demo [here](src/main/kotlin/com/projectronin/interop/soda/SodaDemo.kt).

The Docker image also starts up an instance
of [Oracle's Database Actions](https://localhost:8443/ords/my_atp/admin/_sdw/) that you can use to access the JSON and
SQL views through your browser much like you would in OCI. Use the admin username and the password you set earlier.
