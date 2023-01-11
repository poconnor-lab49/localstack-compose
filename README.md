# localstack-compose

Configuring an application to use virtual host style addressing with localstack in docker compose.

## Developer Quickstart

You need java 17 to build this project.
I suggest using [SDKMAN!](https://sdkman.io/) to manage your java (and various other SDK) versions.
The _.sdkmanrc_ in the root of this project is configured for the latest Java 11.

Install the version of the Java SDK with `sdk env install`

To automatically switch to the configured SDKs, set `sdkman_auto_env=true` in _~/.sdkman/etc/config_

### Build and package

```shell script
$ ./mvnw clean package
$ docker build -f src/main/docker/Dockerfile.jvm -t local/localstack-compose-jvm:edge .
```

### Run localstack

```shell script
docker compose -f docker-compose/docker-compose.yaml up
```

This exposes localstack on port `4566` and the demo application on `8088`

When run from a commandline or IDE, the demo application is available on port `8080`

For a quick check, try

```shell script
./sanity-check.sh 8080
```

And compare with
```shell script
./sanity-check.sh 8088
```

## Notes

There seems to be a bug in AWS CRT or HTTP libraries.
Both the CRT and default `S3AsyncClient`s succeed when we manually add the localstack port to the `Host` header using an nginx proxy.
The default S3AsyncClient works without the `Host` mangling and does not need to go through nginx.

To run with the CRT client, you'll need to target the nginx proxy by setting `S3_ENDPOINT_OVERRIDE: http://s3.localhost.localstack.cloud:8181`

With the default client, both `S3_ENDPOINT_OVERRIDE: http://s3.localhost.localstack.cloud:8181` and `S3_ENDPOINT_OVERRIDE: http://s3.localhost.localstack.cloud:4566` should work.

See [src/main/java/com/example/playground/service/S3Service.java#51](./src/main/java/com/example/playground/service/S3Service.java)
