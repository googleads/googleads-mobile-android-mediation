# This script is to revert changes made within google3 that cause the app not to work correctly using gradle. It reverts changes that have been applied for the new support library.
# Usage: ./dejetify.sh
pushd "$(hg root)/google3"
find "third_party/java_src/gma_sdk_mediation" -type f -name '*.java' -o -name "*.xml" | xargs sed -i'' -f "java/com/google/android/libraries/admob/examples/sed-dejetify-replace.txt"
popd
