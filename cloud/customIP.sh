#!/bin/bash
#set -e

gcloud auth login

gcloud compute networks create my-vpc --subnet-mode=custom
gcloud compute networks subnets create my-subnet --network=my-vpc --region=europe-west1 --range=10.8.0.0/24
gcloud compute networks vpc-access connectors create my-connector --region=europe-west1 --network=my-vpc --range=10.8.0.0/28
gcloud compute addresses create europe-ip --region=europe-west1