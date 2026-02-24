#!/bin/bash

# Check if there are any staged changes in skills/ directory.
if git diff --quiet --cached skills/; then
  exit 0
fi

# Ensure governance files are updated when skills change.
if git diff --quiet --cached skills.allowlist && git diff --quiet --cached skills.versions; then
  echo "Error: Changes detected in 'skills/' but no updates in 'skills.allowlist' or 'skills.versions'."
  exit 1
fi

echo "Skills governance check passed."
exit 0
