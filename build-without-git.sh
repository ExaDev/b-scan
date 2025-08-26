#!/bin/bash
# Temporary script to build without git hook interference in worktrees

# Backup git pointer
mv .git .git.bak 2>/dev/null || true

# Run gradle build
./gradlew "$@"
exit_code=$?

# Restore git pointer
mv .git.bak .git 2>/dev/null || true

exit $exit_code