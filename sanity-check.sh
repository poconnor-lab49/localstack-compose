#!/bin/sh

PORT=${1:-8080}

curl -T <(echo "Hello world!")  http://localhost:${PORT}/s3/filename.txt
#echo
#curl http://localhost:${PORT}/s3/filename.txt
#echo
#curl http://localhost:${PORT}/s3/filename.txt/tags
#echo
