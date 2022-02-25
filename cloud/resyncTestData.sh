#!/bin/bash

gcloud auth login
mkdir -p cloud-test/src/test/resources/data
gsutil -m rsync -r gs://crypto-for-training/data cloud-test/src/test/resources/data