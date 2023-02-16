# localstack-compose

Configuring an application to use virtual host style addressing with localstack in docker compose.

Working through several bugs in the AWS SDK v2

- `Host` header: https://github.com/aws/aws-sdk-java-v2/issues/3682
- NO_PROXY: TODO

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

This exposes localstack on port `4566` and the demo application on port `8088`.

When run from a commandline or IDE, the demo application is available on port `8080`.

For a quick check, try

```shell script
./sanity-check.sh
```

and compare with

```shell script
./sanity-check.sh 8088
```

## Notes

The `Host` header issue from https://github.com/aws/aws-sdk-java-v2/issues/3682 was resolved in AWS SDK version `2.20.x`.

The NO_PROXY issue has not been reproduced yet

The `Main` class is just there to quickly reproduce the issue.
It is not maintained.

