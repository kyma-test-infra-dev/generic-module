#!/usr/bin/env bash

set -e

START_CLASS="com.sap.cloud.connectivity.proxy.operator.ConnectivityProxyOperator"

exec java -classpath /usr/local/share/connectivity-proxy-operator/*: "${START_CLASS}"
