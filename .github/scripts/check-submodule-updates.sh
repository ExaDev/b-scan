#!/bin/bash
set -e

# Script to check for submodule updates and prepare update information
# Parameters: $1 = submodule path

SUBMODULE_PATH="$1"

if [[ -z "$SUBMODULE_PATH" ]]; then
    echo "Error: Submodule path is required"
    exit 1
fi

cd "$SUBMODULE_PATH"

# Get current commit hash
CURRENT_COMMIT=$(git rev-parse HEAD)
echo "current_commit=$CURRENT_COMMIT" >> "$GITHUB_OUTPUT"

# Fetch latest changes from remote
git fetch origin

# Get latest commit hash from default branch
DEFAULT_BRANCH=$(git remote show origin | grep 'HEAD branch' | cut -d' ' -f5)
LATEST_COMMIT=$(git rev-parse origin/$DEFAULT_BRANCH)
echo "latest_commit=$LATEST_COMMIT" >> "$GITHUB_OUTPUT"
echo "default_branch=$DEFAULT_BRANCH" >> "$GITHUB_OUTPUT"

# Check if update is needed
if [[ "$CURRENT_COMMIT" != "$LATEST_COMMIT" ]]; then
    echo "update_needed=true" >> "$GITHUB_OUTPUT"
    echo "Update available for $SUBMODULE_PATH"
    
    # Get commit messages for PR description
    echo "## Changes" > ../submodule-changes.md
    echo "" >> ../submodule-changes.md
    git log --oneline $CURRENT_COMMIT..$LATEST_COMMIT >> ../submodule-changes.md
    
    # Get short commit hashes for branch name
    SHORT_CURRENT=$(echo $CURRENT_COMMIT | cut -c1-7)
    SHORT_LATEST=$(echo $LATEST_COMMIT | cut -c1-7)
    echo "short_current=$SHORT_CURRENT" >> "$GITHUB_OUTPUT"
    echo "short_latest=$SHORT_LATEST" >> "$GITHUB_OUTPUT"
else
    echo "update_needed=false" >> "$GITHUB_OUTPUT"
    echo "No updates available for $SUBMODULE_PATH"
fi