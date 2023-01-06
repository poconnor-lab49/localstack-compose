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

## Things I've tried

Clone and build localstack `master` at commit `d3f51d516af88ab56b06f29ca30bb4142595c8d4`

```shell script
$ cd ../
$ git clone git@github.com:localstack/localstack.git
$ cd localstack
$ make clean install
$ make docker-build
$ docker tag localstack/localstack:latest local/localstack:1.3.2.dev0
```

### Run without ASF

```shell script
$ docker compose -f docker-compose/docker-compose-minimal up
```

Then test endpoints

```shell script
$ aws --endpoint http://localhost.localstack.cloud:4566 s3api list-buckets \
     && aws --endpoint http://localhost.localstack.cloud:4566 s3api create-bucket --bucket test-bucket
$ echo "Text file body" >> testfile.txt
$ curl -T testfile.txt http://localhost.localstack.cloud:4566/test-bucket/testfile.txt
$ aws --endpoint http://localhost.localstack.cloud:4566 s3api list-objects --bucket test-bucket
{
    "Contents": [
        {
            "Key": "testfile.txt",
            "LastModified": "2023-01-06T16:44:57+00:00",
            "ETag": "\"05e863e3ae1d9452c450d9f43333d3fe\"",
            "Size": 15,
            "StorageClass": "STANDARD",
            "Owner": {
                "DisplayName": "webfile",
                "ID": "75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a"
            }
        }
    ]
}

$ curl http://localhost.localstack.cloud:4566/test-bucket/testfile.txt
Text file body
$ curl -H "Host: test-bucket.localhost.localstack.cloud" http://localhost.localstack.cloud:4566/testfile.txt
Text file body
```

Success!!

```shell script
$ docker compose -f docker-compose/docker-compose-minimal down
```

#### Wireshark capture

Capture for `curl -H "Host: test-bucket.localhost.localstack.cloud" http://localhost.localstack.cloud:4566/testfile.txt`

```txt
GET /testfile.txt HTTP/1.1
Host: test-bucket.localhost.localstack.cloud
User-Agent: curl/7.87.0
Accept: */*

HTTP/1.1 200 
content-type: binary/octet-stream
Content-Length: 30
Server: Werkzeug/2.1.2 Python/3.10.8
Date: Fri, 06 Jan 2023 19:53:21 GMT
x-amz-version-id: null
ETag: "182f7c225d23326ab0d40a45bc742925"
last-modified: Fri, 06 Jan 2023 19:53:01 GMT
x-amzn-requestid: 8bjiYJqgGYZdBnszLHwaMB3hQ3wsOmwS2wSaqQdTH0zEkE8Ydmxh
Connection: close
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: HEAD,GET,PUT,POST,DELETE,OPTIONS,PATCH
Access-Control-Allow-Headers: authorization,cache-control,content-length,content-md5,content-type,etag,location,x-amz-acl,x-amz-content-sha256,x-amz-date,x-amz-request-id,x-amz-security-token,x-amz-tagging,x-amz-target,x-amz-user-agent,x-amz-version-id,x-amzn-requestid,x-localstack-target,amz-sdk-invocation-id,amz-sdk-request
Access-Control-Expose-Headers: etag,x-amz-version-id
x-amz-request-id: AA5E586414227106
x-amz-id-2: MzRISOwyjmnupAA5E5864142271067/JypPGXLh0OVFGcJaaO3KW/hRAqKOpIEEp
accept-ranges: bytes
content-language: en-US
date: Fri, 06 Jan 2023 19:53:21 GMT
server: hypercorn-h11

Text file body

```

### Run with ASF

Uncomment `PROVIDER_OVERRIDE_S3: asf` at line 15 in _docker-compose/docker-compose-minimal_

```shell script
$ docker compose -f docker-compose/docker-compose-minimal up
```

Then test endpoints

```shell script
$ aws --endpoint http://localhost.localstack.cloud:4566 s3api list-buckets \
     && aws --endpoint http://localhost.localstack.cloud:4566 s3api create-bucket --bucket test-bucket
$ echo "Text file body" >> testfile.txt
$ curl -T testfile.txt http://localhost.localstack.cloud:4566/test-bucket/testfile.txt
<?xml version='1.0' encoding='utf-8'?>
<PutObjectOutput />
$ aws --endpoint http://localhost.localstack.cloud:4566 s3api list-objects --bucket test-bucket
{
    "Contents": [
        {
            "Key": "testfile.txt",
            "LastModified": "2023-01-06T16:44:57+00:00",
            "ETag": "\"05e863e3ae1d9452c450d9f43333d3fe\"",
            "Size": 15,
            "StorageClass": "STANDARD",
            "Owner": {
                "DisplayName": "webfile",
                "ID": "75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a"
            }
        }
    ]
}

$ curl http://localhost.localstack.cloud:4566/test-bucket/testfile.txt
Text file body
$ curl -H "Host: test-bucket.localhost.localstack.cloud" http://localhost.localstack.cloud:4566/testfile.txt
<?xml version='1.0' encoding='utf-8'?>
<Error><Code>NoSuchBucket</Code><Message>The specified bucket does not exist</Message><RequestId>CC1D26J86EMA9PP8UJY4AMKUBPS0E188KTPMO8HQVECZ58J04LKF</RequestId><BucketName>testfile.txt</BucketName></Error>
```

FAILURE :(

#### Wireshark capture

Capture for `curl -H "Host: test-bucket.localhost.localstack.cloud" http://localhost.localstack.cloud:4566/testfile.txt`

```txt
GET /testfile.txt HTTP/1.1
Host: test-bucket.localhost.localstack.cloud
User-Agent: curl/7.87.0
Accept: */*

HTTP/1.1 404 
Content-Type: application/xml
Content-Length: 245
x-amz-request-id: I7SC618TVR8RG5GTV0N3U4UF7T3ZOHK6IBCJE96GE28CNLAEDNNQ
x-amz-id-2: MzRISOwyjmnupI7SC618TVR8RG5GTV0N3U4UF7T3ZOHK6IBCJE96GE28CNLAEDNNQ7/JypPGXLh0OVFGcJaaO3KW/hRAqKOpIEEp
Connection: close
date: Fri, 06 Jan 2023 19:57:24 GMT
server: hypercorn-h11

<?xml version='1.0' encoding='utf-8'?>
<Error><Code>NoSuchBucket</Code><Message>The specified bucket does not exist</Message><RequestId>LRQXDRX5FM6MQW2O6GQAZL4JKHAEFSUA0YQJW61TTNG32MIHOFMS</RequestId><BucketName>testfile.txt</BucketName></Error>
```
