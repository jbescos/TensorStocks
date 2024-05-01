#!/bin/bash
set -e

if [ $# -lt 1 ]; then
  echo 1>&2 "$0: Pattern to download CSV is necessary. For example: ./downloadBinance.sh 2024-04*"
  exit 2
else
  PATTERN=$1
  scp jbescos@192.168.0.193:~/bot/crypto-for-training/data/binance/${PATTERN} cloud-test/data/binance
fi

