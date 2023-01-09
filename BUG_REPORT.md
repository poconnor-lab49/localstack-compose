# bug: S3 ASF Provider does not appear to support virtual-host style requests

## Current Behavior

Using the S3 ASF provider, trying to fetch an object using S3 virtual-host style requests like

`curl -H "Host: test-bucket.localhost.localstack.cloud" http://localhost.localstack.cloud:4566/testfile.txt`

results in an error

```xml
<?xml version='1.0' encoding='utf-8'?>
<Error><Code>NoSuchBucket</Code><Message>The specified bucket does not exist</Message><RequestId>CC1D26J86EMA9PP8UJY4AMKUBPS0E188KTPMO8HQVECZ58J04LKF</RequestId><BucketName>testfile.txt</BucketName></Error>
```

Path style requests like `curl http://localhost.localstack.cloud:4566/test-bucket/testfile.txt` work fine, and so does the legacy S3 provider.

## Expected Behavior

Path and Virtual Host requests work the same in both providers

## Steps to Reproduce

#### How are you starting localstack (e.g., `bin/localstack` command, arguments, or `docker-compose.yml`)
`docker compose up`

Using _docker-compose.yaml_

```yaml
services:
  ###
  # AWS services
  # localstack home: https://github.com/localstack/localstack
  ###
  localstack:
    image: local/localstack:1.3.2.dev0
    ports:
      - 4566:4566
    expose:
      - 4566
    networks:
      curl:
    environment:
      PROVIDER_OVERRIDE_S3: asf
      EAGER_SERVICE_LOADING: 1
      SERVICES: s3
      DEBUG: ${DEBUG_LOCALSTACK:-0}
networks:
  curl:
```

#### Client commands (e.g., AWS SDK code snippet, or sequence of "awslocal" commands)

```sh
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

## Environment

- OS: Slackware-current (5.19.17)
- LocalStack: latest (d3f51d516af88ab56b06f29ca30bb4142595c8d4)

## Anything else?

Comment out `PROVIDER_OVERRIDE_S3: asf`  in the _docker-compose.yaml_ file and everything works fine.
