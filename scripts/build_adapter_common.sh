#!/bin/bash
# Helper functions to generate and upload Android adapter releases.
# Prerequisites:
# 1. Requires a fig client
# 2. Requires gcert

# Stop if any command fails.
set -e
# Stop if any command in a pipe fails.
set -o pipefail

# Builds and uploads an Android mediation adapter release.
# Sample usage: build_and_upload_adapter facebook
build_and_upload_adapter() {
  local usage="Builds and uploads an Android mediation adapter release.\n
     Usage:\n
     build_and_upload_adapter <adapter>"
  if [ -z "$1" ]; then
   echo -e $usage
   exit 2
  fi

  # Check that the script is being invoked at head. See
  # https://stackoverflow.com/questions/59895#answer-246128 for more
  # information on getting the script dir.
  local script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
  local expected_script_dir="/google/src/head/depot/google3/third_party/java_src/gma_sdk_mediation/scripts"
  if [ ${script_dir} != ${expected_script_dir} ]; then
    echo "Error: Expected script to be called invoked from head using /google/src/head/depot/google3/..."
    exit 2
  fi

  local adapter=$1
  echo "Building and uploading release for ${adapter} adapter"

  local client_name="${adapter}-adapter-release-${RANDOM}"
  echo "Creating new client with name ${client_name}"
  hg citc ${client_name}

  # Before changing working directories, set up cleanup script
  # to make sure we always pop back to current directory.
  trap cleanup EXIT
  pushd .

  cd $(hg hgd ${client_name})
  local google3_dir=$(pwd)

  local gma_sdk_mediation_dir="${google3_dir}/third_party/java_src/gma_sdk_mediation"
  local adapter_dir="${gma_sdk_mediation_dir}/third_party_adapters/${adapter}"

  # Build the release.
  local adapter_version=$(cat "${adapter_dir}/${adapter}/build.gradle" | grep "stringVersion =" | sed -e 's/.*"\(.*\)".*/\1/')
  echo "Building ${adapter} adapter version ${adapter_version}"
  ${google3_dir}/third_party/gradle/wrapper_files/gradlew packageDistribution -p ${adapter_dir}

  # Move release to download folder.
  echo "Moving release to download folder"
  local path_to_binary="$(find ${adapter_dir}/build/distribution | grep AndroidAdapter.*\.zip)"
  local release_dir="${google3_dir}/googledata/download/googleadmobadssdk/mediation/android/${adapter}"
  mkdir -p ${release_dir}
  cp ${path_to_binary} ${release_dir}

  # Create commit and upload.
  echo "Creating release CL"
  hg add ${release_dir}
  hg commit -m "#Maintenance Adds Release CL for ${adapter} adapter version ${adapter_version}"
  hg upload chain
}

cleanup() {
  echo "Cleaning up"
  popd >> /dev/null
}