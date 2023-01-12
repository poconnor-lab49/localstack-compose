# S3TransferManager CRT AsyncClient in version 2.9.12 does not set port in `Host` header when using endpointOverride

[AWS Bug report](https://github.com/aws/aws-sdk-java-v2/issues/3682)

## Describe the bug

The S3TransferManager does not include the port in the `Host` header when using the `S3AsyncClient.crtBuilder()` and an endpointOverride.


## Expected Behavior

The endpoint port is included in the `Host` header if it is not the default for the protocol.

### Wireshark

```http
HTTP/1.1 100 Continue
date: Wed, 11 Jan 2023 15:50:17 GMT
server: hypercorn-h11

PUT /filename.txt HTTP/1.1
Host: test-bucket.s3.localhost.localstack.cloud:4566
... Clipping various amazon headers ...
Content-Length: 18
Content-Type: text/plain; charset=UTF-8
Expect: 100-continue
User-Agent: aws-sdk-java/2.19.12 Linux/5.19.17 OpenJDK_64-Bit_Server_VM/17.0.5+8 Java/17.0.5 vendor/Eclipse_Adoptium io/async http/NettyNio cfg/retry-mode/legacy ft/s3-transfer
x-amz-tagging: MyTag=TaggingWorks

Test file contentsHTTP/1.1 200 
Content-Type: application/xml
ETag: "cc92658b10d5a09dfbed5af10ac6105f"
Content-Length: 58
... Clipping various amazon headers ...
Connection: close
date: Wed, 11 Jan 2023 15:50:17 GMT
server: hypercorn-h11

<?xml version='1.0' encoding='utf-8'?>
<PutObjectOutput />
```

### Notes to self

[According to MDN](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/host), the `Host` header should include the port unless it is the default for the protocol.
It's less clear in [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html#name-host-and-authority) than it was in [RFC 2616](https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.23).
Not sure if its still spelled out anywhere.

## Current Behavior

The CRT client does not set the port from the endpoint override.
The legacy client sets the port correctly.

## wireshark


```http
PUT /filename.txt HTTP/1.1
Host: test-bucket.s3.localhost.localstack.cloud
... Clipping various amazon headers ...
Content-Type: text/plain; charset=UTF-8
Expect: 100-continue
x-amz-tagging: MyTag=TaggingWorks
Content-Encoding: aws-chunked
Content-Length: 60
User-Agent: aws-sdk-java/2.19.12 Linux/5.19.17 OpenJDK_64-Bit_Server_VM/17.0.5+8 Java/17.0.5 vendor/Eclipse_Adoptium io/async http/s3crt cfg/retry-mode/legacy ft/s3-transfer CRTS3NativeClient/0.1.x

HTTP/1.1 100 Continue
date: Wed, 11 Jan 2023 15:51:25 GMT
server: hypercorn-h11

HTTP/1.1 500 
Content-Type: application/xml
Content-Length: 521
... Clipping various amazon headers ...
Connection: close
date: Wed, 11 Jan 2023 15:51:25 GMT
server: hypercorn-h11

<?xml version='1.0' encoding='utf-8'?>
<Error><Code>InternalError</Code><Message>exception while calling s3 with unknown operation: MyHTTPConnectionPool(host='s3.localhost.localstack.cloud', port=80): Max retries exceeded with url: /test-bucket/filename.txt (Caused by NewConnectionError('&lt;urllib3.connection.HTTPConnection object at 0x7fb42cf8d030&gt;: Failed to establish a new connection: [Errno 111] Connection refused'))</Message><RequestId>DABOJY37POD0F9AT3DJH2DBHB67VS4JBZTI287AM4T3GV5LZ23ZJ</RequestId></Error>
```

### Log

```log
Exception in thread "main" java.util.concurrent.CompletionException: software.amazon.awssdk.core.exception.SdkClientException: Failed to send the request: Response code indicates internal server error
    at software.amazon.awssdk.utils.CompletableFutureUtils.errorAsCompletionException(CompletableFutureUtils.java:65)
    at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncExecutionFailureExceptionReportingStage.lambda$execute$0(AsyncExecutionFailureExceptionReportingStage.java:51)
    at java.base/java.util.concurrent.CompletableFuture.uniHandle(CompletableFuture.java:934)
    at java.base/java.util.concurrent.CompletableFuture$UniHandle.tryFire(CompletableFuture.java:911)
    at java.base/java.util.concurrent.CompletableFuture.postComplete(CompletableFuture.java:510)
    at java.base/java.util.concurrent.CompletableFuture.completeExceptionally(CompletableFuture.java:2162)
    at software.amazon.awssdk.utils.CompletableFutureUtils.lambda$forwardExceptionTo$0(CompletableFutureUtils.java:79)
    at java.base/java.util.concurrent.CompletableFuture.uniWhenComplete(CompletableFuture.java:863)
    at java.base/java.util.concurrent.CompletableFuture$UniWhenComplete.tryFire(CompletableFuture.java:841)
    at java.base/java.util.concurrent.CompletableFuture.postComplete(CompletableFuture.java:510)
    at java.base/java.util.concurrent.CompletableFuture.completeExceptionally(CompletableFuture.java:2162)
    at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage$RetryingExecutor.maybeAttemptExecute(AsyncRetryableStage.java:103)
    at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage$RetryingExecutor.maybeRetryExecute(AsyncRetryableStage.java:184)
    at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage$RetryingExecutor.lambda$attemptExecute$1(AsyncRetryableStage.java:159)
    at java.base/java.util.concurrent.CompletableFuture.uniWhenComplete(CompletableFuture.java:863)
    at java.base/java.util.concurrent.CompletableFuture$UniWhenComplete.tryFire(CompletableFuture.java:841)
    at java.base/java.util.concurrent.CompletableFuture.postComplete(CompletableFuture.java:510)
    at java.base/java.util.concurrent.CompletableFuture.completeExceptionally(CompletableFuture.java:2162)
    at software.amazon.awssdk.utils.CompletableFutureUtils.lambda$forwardExceptionTo$0(CompletableFutureUtils.java:79)
    at java.base/java.util.concurrent.CompletableFuture.uniWhenComplete(CompletableFuture.java:863)
    at java.base/java.util.concurrent.CompletableFuture$UniWhenComplete.tryFire(CompletableFuture.java:841)
    at java.base/java.util.concurrent.CompletableFuture.postComplete(CompletableFuture.java:510)
    at java.base/java.util.concurrent.CompletableFuture.completeExceptionally(CompletableFuture.java:2162)
    at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.lambda$null$0(MakeAsyncHttpRequestStage.java:103)
    at java.base/java.util.concurrent.CompletableFuture.uniWhenComplete(CompletableFuture.java:863)
    at java.base/java.util.concurrent.CompletableFuture$UniWhenComplete.tryFire(CompletableFuture.java:841)
    at java.base/java.util.concurrent.CompletableFuture.postComplete(CompletableFuture.java:510)
    at java.base/java.util.concurrent.CompletableFuture.completeExceptionally(CompletableFuture.java:2162)
    at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.lambda$executeHttpRequest$3(MakeAsyncHttpRequestStage.java:165)
    at java.base/java.util.concurrent.CompletableFuture.uniWhenComplete(CompletableFuture.java:863)
    at java.base/java.util.concurrent.CompletableFuture$UniWhenComplete.tryFire(CompletableFuture.java:841)
    at java.base/java.util.concurrent.CompletableFuture$Completion.run(CompletableFuture.java:482)
    at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
    at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
    at java.base/java.lang.Thread.run(Thread.java:833)
Caused by: software.amazon.awssdk.core.exception.SdkClientException: Failed to send the request: Response code indicates internal server error
    at software.amazon.awssdk.core.exception.SdkClientException$BuilderImpl.build(SdkClientException.java:111)
    at software.amazon.awssdk.core.exception.SdkClientException.create(SdkClientException.java:43)
    at software.amazon.awssdk.services.s3.internal.crt.S3CrtResponseHandlerAdapter.handleError(S3CrtResponseHandlerAdapter.java:127)
    at software.amazon.awssdk.services.s3.internal.crt.S3CrtResponseHandlerAdapter.onFinished(S3CrtResponseHandlerAdapter.java:93)
    at software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandlerNativeAdapter.onFinished(S3MetaRequestResponseHandlerNativeAdapter.java:24)
```

## Steps to Reproduce

Run localstack in docker compose:

```yaml
services:
  ###
  # AWS services
  # localstack home: https://github.com/localstack/localstack
  ###
  localstack:
    image: localstack/localstack:latest
    ports:
      - 4566:4566
    expose:
      - 4566
    networks:
      bug:
    environment:
      PROVIDER_OVERRIDE_S3: asf
      EAGER_SERVICE_LOADING: 1
      SERVICES: s3
networks:
  bug:
```

Create a bucket

```sh
aws --endpoint http://localhost:4566 s3api create-bucket --bucket test-bucket
```

Try to PUT and GET an object

```java
import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;

/**
 * Minimal reproducer for bug
 *
 */
public class Main {
  private static final String BUCKET = "test-bucket";
  private static final String KEY = "testfile.txt";

  public static void main(String... args) {
    // resolves to 127.0.0.1
    var endpoint = "http://s3.localhost.localstack.cloud:4566";

    // change this to `S3AsyncClient.builder()` and everything works
//    var client = S3AsyncClient.builder()
    var client = S3AsyncClient.crtBuilder()
        .credentialsProvider(() -> AwsBasicCredentials.create("localstack", "localstack"))
        .endpointOverride(URI.create(endpoint))
        .build();

    var putRequest = PutObjectRequest.builder()
        .bucket(BUCKET).key(KEY)
        .tagging("MyTag=TaggingWorks")
        .build();

    var s3TransferManager = S3TransferManager.builder()
        .s3Client(client)
        .build();

    var upload = s3TransferManager.upload(ub -> ub.putObjectRequest(putRequest)
        .requestBody(AsyncRequestBody.fromString("File contents")));

    upload.completionFuture().join();

    var d = DownloadRequest.builder().getObjectRequest(t -> t.bucket(BUCKET).key(KEY).build())
        .responseTransformer(AsyncResponseTransformer.toBytes())
        .build();

    var download = s3TransferManager.download(d);

    System.out.println(download.completionFuture().join().result().asUtf8String());
  }

}
```
## Possible Solution

Maybe something is broken in https://github.com/awslabs/aws-c-http or https://github.com/awslabs/aws-crt-java ?


## Additional Information/Context

Used dependencies as described in https://aws.amazon.com/blogs/developer/introducing-crt-based-s3-client-and-the-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/

SDK: 2.19.12
CRT: 0.21.0K

This can be worked around using a proxy to add the port to the header.  See https://github.com/localstack/localstack/issues/7449#issuecomment-1374859887 for an example.

Possibly related:

- https://github.com/aws/aws-sdk-java-v2/issues/3350

Tangentially related and maybe in the wrong repo:

- https://github.com/awslabs/aws-c-http/issues/413

## AWS Java SDK version used

2.19.12

## JDK version used

```sh
$ java -version
openjdk version "17.0.5" 2022-10-18
OpenJDK Runtime Environment Temurin-17.0.5+8 (build 17.0.5+8)
OpenJDK 64-Bit Server VM Temurin-17.0.5+8 (build 17.0.5+8, mixed mode, sharing)
```

## Operating System and version

slackware-current
