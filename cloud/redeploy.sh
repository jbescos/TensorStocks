#!/bin/bash
#set -e

BUCKET="crypto-packages"
SOURCES="gs://${BUCKET}/"

compileCode() {
  mvn clean install -DskipTests
}
deleteFunctions() {
  echo y | gcloud functions delete function-crypto-storage --project=${PROJECT}
  echo y | gcloud functions delete bot --project=${PROJECT}
  echo y | gcloud functions delete chart --project=${PROJECT}
  echo y | gcloud functions delete chartFileListener --project=${PROJECT}
}
uploadZips() {
  gcloud storage cp packages/cloud-bot/target/cloud-bot-1.0.zip ${SOURCES}
}
createFunctions() {
  gcloud functions deploy function-crypto-storage --trigger-http --runtime=java11 --allow-unauthenticated --memory=512MB --source=${SOURCES}cloud-bot-1.0.zip --stage-bucket=${BUCKET} --timeout=280s --entry-point=com.jbescos.cloudstorage.StorageFunction --project=${PROJECT}
  gcloud functions deploy bot --trigger-topic=storage --runtime=java11 --memory=512MB --source=${SOURCES}cloud-bot-1.0.zip --stage-bucket=${BUCKET} --timeout=280s --entry-point=com.jbescos.cloudbot.BotSubscriber --project=${PROJECT}
  gcloud functions deploy chart --trigger-http --runtime=java11 --allow-unauthenticated --memory=512MB --source=${SOURCES}cloud-bot-1.0.zip --stage-bucket=${BUCKET} --timeout=280s --entry-point=com.jbescos.cloudchart.ChartFunction --project=${PROJECT}
  gcloud functions deploy chartFileListener --trigger-event=google.storage.object.finalize --trigger-resource=crypto-for-training --runtime=java11 --memory=512MB --source=${SOURCES}cloud-bot-1.0.zip --stage-bucket=${BUCKET} --timeout=280s --entry-point=com.jbescos.cloudchart.ChartFileListener --project=${PROJECT}
}

if [ $# -lt 1 ]; then
  echo 1>&2 "$0: Project id is required"
  exit 2
else
  PROJECT=$1
  echo "Project $1"
  gcloud auth login
  compileCode
  deleteFunctions
  uploadZips
  createFunctions
fi
