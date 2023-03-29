#!/bin/bash

CURRENT_DIR=`pwd`
PROPERTIES_SRC="$HOME/bot/crypto-properties/cloud.properties"
GIT_SRC="$HOME/workspace/TensorStocks"
cd $GIT_SRC
git pull --force
cp $GIT_SRC/cloud/cloud-test/src/main/resources/cloud.properties $PROPERTIES_SRC
cd $CURRENT_DIR

# Run only Binance
timeout 280s java -jar $HOME/workspace/TensorStocks/cloud/packages/local-bot/target/local-bot-1.0.jar BINANCE
