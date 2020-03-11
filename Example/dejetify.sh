# This script is to revert changes made within google3 that cause the app not to work correctly using gradle. It reverts changes that have been applied for the new support library.
# Usage: ./dejetify.sh

find adapter/src -type f -name '*.java' -o -name "*.xml" | xargs sed -i '' -f "sed-dejetify-replace.txt"
find app/src -type f -name '*.java' -o -name "*.xml" | xargs sed -i '' -f "sed-dejetify-replace.txt"
find customevent/src -type f -name '*.java' -o -name "*.xml" | xargs sed -i '' -f "sed-dejetify-replace.txt"
find sdk/src -type f -name '*.java' -o -name "*.xml" | xargs sed -i '' -f "sed-dejetify-replace.txt"
