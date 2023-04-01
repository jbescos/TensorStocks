#!/bin/bash

gcloud auth login
mkdir -p cloud-test/data
gsutil -m rsync -r gs://crypto-for-training/data cloud-test/data