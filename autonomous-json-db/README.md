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
--hostname localhost \
--cap-add SYS_ADMIN \
--device /dev/fuse \
--name adb_container \
container-registry.oracle.com/database/adb-free:latest
```

Note the container ID returned by the above command, or look it up using `docker ps`, for the following commands.

If this is a new instance, you will need to set a new admin password. This example sets it to the one used for the
demo code, but feel free to select your own.
`docker exec [CONTAINER_ID] /u01/scripts/change_expired_password.sh
MY_ATP admin Welcome_MY_ATP_1234 Longpassword1`

In order for this demo to run, we need to have our Wallet setup. Create /tmp/scratch on your local machine, and then run
the following
`docker cp [CONTAINER_ID]:/u01/app/oracle/wallets/tls_wallet /tmp/scratch/tls_wallet`

## Demo

You can see the code demo [here](src/main/kotlin/com/projectronin/interop/soda/SodaDemo.kt).

The Docker image also starts up an instance
of [Oracle's Database Actions](https://localhost:8443/ords/my_atp/admin/_sdw/) that you can use to access the JSON and
SQL views through your browser much like you would in OCI. Use the admin username and the password you set earlier.
