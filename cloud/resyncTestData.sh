#!/bin/bash

gcloud auth login
gsutil -m rsync -r gs://crypto-for-training/data cloud-test/src/test/resources/data