#!/bin/bash

S3_HOST=${1}
aws --endpoint http://${S3_HOST}:4566 s3api list-buckets \
     && aws --endpoint http://${S3_HOST}:4566 s3api create-bucket --bucket test-bucket
echo $(date --iso-8601=seconds) >| testfile.txt
curl -T testfile.txt http://${S3_HOST}:4566/test-bucket/testfile.txt
curl http://${S3_HOST}:4566/test-bucket/testfile.txt
HTTP_CODE=$(curl -H "Host: test-bucket.${S3_HOST}" http://${S3_HOST}:4566/testfile.txt -o /dev/null -w "%{http_code}\n" --silent)

if [[ "${HTTP_CODE}" -eq "200" ]]; then
  echo "success"
else
  echo "failure"
  exit 1
fi
