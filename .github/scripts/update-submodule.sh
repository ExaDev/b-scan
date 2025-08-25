#!/bin/bash
set -e

# Script to create update branch and commit submodule changes
# Parameters: $1 = submodule path, $2 = submodule name, $3 = latest commit, $4 = short current, $5 = short latest

SUBMODULE_PATH="$1"
SUBMODULE_NAME="$2"
LATEST_COMMIT="$3"
SHORT_CURRENT="$4"
SHORT_LATEST="$5"

if [[ -z "$SUBMODULE_PATH" || -z "$SUBMODULE_NAME" || -z "$LATEST_COMMIT" || -z "$SHORT_CURRENT" || -z "$SHORT_LATEST" ]]; then
    echo "Error: All parameters are required"
    echo "Usage: $0 <submodule_path> <submodule_name> <latest_commit> <short_current> <short_latest>"
    exit 1
fi

# Create a new branch for this update
BRANCH_NAME="submodule/update-$(echo "$SUBMODULE_PATH" | tr '/' '-')-$SHORT_LATEST"
echo "branch_name=$BRANCH_NAME" >> "$GITHUB_ENV"

git checkout -b "$BRANCH_NAME"

# Update the submodule
cd "$SUBMODULE_PATH"
git checkout "$LATEST_COMMIT"

cd ..
git add "$SUBMODULE_PATH"

# Commit the submodule update
git commit -m "feat(submodule): update $SUBMODULE_NAME to latest version

Update $SUBMODULE_PATH from $SHORT_CURRENT to $SHORT_LATEST

This automated update includes the latest changes from the upstream repository."

# Push the branch
git push origin "$BRANCH_NAME"

echo "Created and pushed branch: $BRANCH_NAME"