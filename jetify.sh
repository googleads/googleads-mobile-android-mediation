# This script is to revert changes made by dejetify.sh that to enable the app to work correctly using gradle. It re-applies changes that have been applied for the new support library.
# Usage: ./jetify.sh
pushd "$(hg root)/google3"
find "third_party/java_src/gma_sdk_mediation" -type f -name '*.java' -o -name "*.xml" | xargs sed -i'' -f "java/com/google/android/libraries/admob/examples/sed-jetify-replace.txt"
popd
