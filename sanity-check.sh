#!/bin/sh

PORT=${1}

curl -XPUT http://localhost:${PORT}/s3/filename.txt
echo
curl http://localhost:${PORT}/s3/filename.txt
echo
curl http://localhost:${PORT}/s3/filename.txt/tags
echo
