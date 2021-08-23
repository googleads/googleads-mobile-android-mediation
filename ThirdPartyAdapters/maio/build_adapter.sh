#!/bin/bash
# Script to create a Maio Android adapter release.
# Prerequisites:
# 1. Requires a fig client
# 2. Requires gcert

source "/google/src/head/depot/google3/third_party/java_src/gma_sdk_mediation/scripts/build_adapter_common.sh"
# Uncomment for testing
# GOOGLE3=$(pwd | sed "s,^\(.*\)google3.*$,\1google3,")
# source "${GOOGLE3}/third_party/java_src/gma_sdk_mediation/scripts/build_adapter_common.sh"

build_and_upload_adapter "maio"