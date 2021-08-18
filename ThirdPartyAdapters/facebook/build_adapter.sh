#!/bin/bash
# Script to create a Facebook Android adapter release.
# Prerequisites:
# 1. Requires a fig client
# 2. Requires gcert

# Stop if any command fails.
set -e
# Stop if any command in a pipe fails.
set -o pipefail

GOOGLE3=$(pwd | sed "s,^\(.*\)google3.*$,\1google3,")
source "${GOOGLE3}/googlemac/iPhone/GoogleAds/GoogleMobileAdsNonagon/Nonagon/scripts/common.sh"

echo "Syncing client..."
hg sync

# Do not allow releases to be built from dirty clients.
echo "Checking that client is clean..."
gad_assert_clean_client

# Copy in gradle-wrapper files from 3rd_party.
echo "Pulling in gradle-wrapper files from 3rd party..."
rsync -r "${GOOGLE3}/third_party/gradle/wrapper_files/" .

# Make sure gradlew is executable.
chmod +x gradlew

# Remove old builds. Ignore errors.
echo "Cleaning out old adapter files..."
rm -rf build/distribution >> /dev/null 2>&1 || true

# Build the release.
echo "Building the release..."
./gradlew packageDistribution

# Revert changes to any gradle-wrapper files.
hg revert .

# Create a CL.
create_cl() {
  echo "Creating CL..."
  # Move release to download folder.
  PATH_TO_BINARY="$(find build/distribution | grep FacebookAndroidAdapter.*\.zip)"
  RELEASE_FOLDER="${GOOGLE3}/googledata/download/googleadmobadssdk/mediation/android/facebook"
  cp ${PATH_TO_BINARY} ${RELEASE_FOLDER}

  # Create commit and upload.
  ADAPTER_VERSION=$(cat facebook/build.gradle | grep "stringVersion =" | sed -e 's/.*"\(.*\)".*/\1/')
  hg add ${RELEASE_FOLDER}
  hg commit -m "#Maintenance Adds Release CL for Facebook adapter version ${ADAPTER_VERSION}"
  hg upload chain
}

create_cl
