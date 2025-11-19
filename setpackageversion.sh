#!/usr/bin/env bash
function set_packages_version() {
  if [ -z "$1" ]; then
      echo "Missing version parameter" >&2
      exit 1
  fi

  version="$1"
  current_directory="$(dirname "$(readlink -f "$0")")/.."


  find "$current_directory" -type d -name "node_modules" -prune -o -name "package.json" -print | \
  grep -v "^$current_directory/package.json$" | \
  while IFS= read -r pkg_path; do

      ## private or unnamed packages are skipped
      is_skipped=$(jq -r '.private' "$pkg_path")

      if [ "$is_skipped" = "true" ]; then
          continue
      fi

      # Create a temporary file in case of issue during the version patch
      temp_file=$(mktemp)

      # Update the version with jq on the temp file, and replace the original file with the patch version if no
      # problem was encountered.
      if ! jq ".version = \"$version\"" "$pkg_path" > "$temp_file"; then
          relative_pkg_path=$(realpath --relative-to="$current_directory" "$pkg_path")
          echo "Failed to update $relative_pkg_path: jq error" >&2
          rm -f "$temp_file"
          continue
      fi

      mv "$temp_file" "$pkg_path"
  done
}

set_packages_version 0.0.1