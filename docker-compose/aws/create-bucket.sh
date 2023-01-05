#!/bin/sh

echo "initializing S3"
awslocal s3api create-bucket --bucket test-bucket --region us-east-1
