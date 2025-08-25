#!/bin/bash
set -e

# Script to detect all submodules and output them as JSON for GitHub Actions matrix

# Check if submodules exist
if ! git submodule status --recursive > /dev/null 2>&1; then
    echo "No submodules found"
    echo "submodules=[]" >> "$GITHUB_OUTPUT"
    exit 0
fi

# Build JSON array of submodules
submodules_json="["
first=true

while IFS= read -r line; do
    if [[ $line =~ ^submodule\.([^.]+)\.path=(.+)$ ]]; then
        name="${BASH_REMATCH[1]}"
        path="${BASH_REMATCH[2]}"
        
        url=$(git config -f .gitmodules --get "submodule.$name.url" 2>/dev/null || echo "unknown")
        repo_name=$(basename "$url" .git)
        
        if [[ $first == true ]]; then
            first=false
        else
            submodules_json+=","
        fi
        
        submodules_json+="{\"name\":\"$name\",\"path\":\"$path\",\"description\":\"Update $repo_name with latest upstream changes\"}"
    fi
done < <(git config -f .gitmodules --list | grep -E '^submodule\..*\.path=')

submodules_json+="]"

echo "Discovered submodules: $submodules_json"
echo "submodules=$submodules_json" >> "$GITHUB_OUTPUT"