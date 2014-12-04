#!/bin/bash  
export LD_LIBRARY_PATH=/usr/lib

BIN_DIRECTORY=`dirname $0`

java -Djava.library.path=$BIN_DIRECTORY -classpath $BIN_DIRECTORY/../target/libpcap-latency-meter-0.2RC1.jar:$BIN_DIRECTORY/jnetpcap.jar org.tinyfix.latency.LiveCaptureProcessor "$@"
