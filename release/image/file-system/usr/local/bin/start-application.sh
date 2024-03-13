#!/usr/bin/env bash

set -e

app_to_start="${START_APPLICATION}"

case "${app_to_start}" in
	"proxy-operator")
		exec "start-proxy-operator.sh"
	;;
esac
