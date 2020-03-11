# This script is to revert changes made by dejetify.sh that to enable the app to work correctly using gradle. It re-applies changes that have been applied for the new support library.
# Usage: ./jetify.sh

find adapter/src -type f -name '*.java' -o -name "*.xml" | xargs sed -i '' -f "sed-jetify-replace.txt"
find app/src -type f -name '*.java' -o -name "*.xml" | xargs sed -i '' -f "sed-jetify-replace.txt"
find customevent/src -type f -name '*.java' -o -name "*.xml" | xargs sed -i '' -f "sed-jetify-replace.txt"
find sdk/src -type f -name '*.java' -o -name "*.xml" | xargs sed -i '' -f "sed-jetify-replace.txt"
