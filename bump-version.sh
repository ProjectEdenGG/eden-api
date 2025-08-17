#!/bin/bash

# Function to increment semantic version
increment_version() {
    local version=$1
    local position=$2  # major, minor, or patch

    # Remove SNAPSHOT suffix if present
    local is_snapshot=false
    if [[ $version == *"-SNAPSHOT" ]]; then
        is_snapshot=true
        version=${version%-SNAPSHOT}
    fi

    # Split version into components
    IFS='.' read -r major minor patch <<< "$version"

    # Increment the appropriate component
    case $position in
        major)
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        minor)
            minor=$((minor + 1))
            patch=0
            ;;
        patch)
            patch=$((patch + 1))
            ;;
        *)
            echo "Invalid position: $position. Use 'major', 'minor', or 'patch'."
            exit 1
            ;;
    esac

    # Reconstruct the version
    local new_version="${major}.${minor}.${patch}"

    # Add SNAPSHOT suffix back if it was present
    if [ "$is_snapshot" = true ]; then
        new_version="${new_version}-SNAPSHOT"
    fi

    echo "$new_version"
}

# Parse command line arguments
position="patch"  # Default to incrementing patch version
dry_run=false

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --major) position="major"; shift ;;
        --minor) position="minor"; shift ;;
        --patch) position="patch"; shift ;;
        --dry-run) dry_run=true; shift ;;
        *) echo "Unknown parameter: $1"; exit 1 ;;
    esac
done

# Find the root POM file
root_pom="pom.xml"
if [ ! -f "$root_pom" ]; then
    echo "Root POM file not found. Make sure you're running this script from the project root directory."
    exit 1
fi

# Extract current version from root POM using a more compatible approach
# First look for project/version tag
current_version=$(sed -n 's/.*<version>\(.*\)<\/version>.*/\1/p' "$root_pom" | head -1)

# If not found, try looking for parent/version tag
if [ -z "$current_version" ]; then
    current_version=$(sed -n 's/.*<parent>.*<version>\(.*\)<\/version>.*/\1/p' "$root_pom")
fi

if [ -z "$current_version" ]; then
    echo "Could not find version in root POM."
    exit 1
fi

# Calculate the next version
next_version=$(increment_version "$current_version" "$position")

echo "Current version: $current_version"
echo "Next version: $next_version"

if [ "$dry_run" = true ]; then
    echo "Dry run - no changes will be made."
    exit 0
fi

# Find all POM files in the project
pom_files=$(find . -name "pom.xml")

# Update the version in all POM files
for pom_file in $pom_files; do
    echo "Updating $pom_file"

    # Use sed for in-place editing - more portable than perl across systems
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS requires an extension for backup
        sed -i '' "s/<version>$current_version<\/version>/<version>$next_version<\/version>/" "$pom_file"
    else
        # Linux/Unix
        sed -i "s/<version>$current_version<\/version>/<version>$next_version<\/version>/" "$pom_file"
    fi
done

echo "All POM files updated to version $next_version"