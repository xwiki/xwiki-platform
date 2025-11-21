#!/usr/bin/env node

/*
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import fs from 'fs';
import {execSync} from 'child_process';
import path from 'path';

/**
 * Script to conditionally publish npm packages based on version type (SNAPSHOT vs release)
 * Step 1: Updates all package.json files with the same timestamp
 * Step 2: Uses pnpm -r publish to publish all packages
 * Usage: node publish-package.js <snapshot-registry> <release-registry> [base-directory]
 */

// Read command line arguments
const snapshotRegistry = process.argv[2];
const releaseRegistry = process.argv[3];
const baseDirectory = process.argv[4] || process.cwd();

if (!snapshotRegistry || !releaseRegistry) {
  console.error('Error: Missing required arguments');
  console.error('Usage: node publish-package.js <snapshot-registry> <release-registry> [base-directory]');
  process.exit(1);
}

/**
 * Recursively find all package.json files in subdirectories
 * @param {string} dir - Directory to search
 * @param {string[]} fileList - Accumulated list of package.json paths
 * @returns {string[]} - Array of package.json file paths
 */
function findPackageJsonFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);

  files.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      // Skip node_modules and hidden directories
      if (file !== 'node_modules' && !file.startsWith('.')) {
        findPackageJsonFiles(filePath, fileList);
      }
    } else if (file === 'package.json') {
      fileList.push(filePath);
    }
  });

  return fileList;
}

/**
 * Update version in a package.json file
 * @param {string} packageJsonPath - Path to package.json
 * @param {Object} packageJson - Parsed package.json content
 * @param {string} timestamp - Timestamp to replace SNAPSHOT with
 * @returns {Object} - {success, originalVersion, newVersion, packageName, isSnapshot}
 */
function updatePackageVersion(packageJsonPath, packageJson, timestamp) {
  const originalVersion = packageJson.version;
  const packageName = packageJson.name;
  const isSnapshot = originalVersion.includes('SNAPSHOT');

  if (isSnapshot) {
    // Replace SNAPSHOT with timestamp
    const newVersion = originalVersion.replace('SNAPSHOT', timestamp);
    packageJson.version = newVersion;

    try {
      fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2) + '\n');
      return { success: true, originalVersion, newVersion, packageName, isSnapshot: true };
    } catch (error) {
      return { success: false };
    }
  } else {
    return { success: true, originalVersion, newVersion: originalVersion, packageName, isSnapshot: false };
  }
}

/**
 * Restore original versions in all package.json files
 * @param {Array} updates - Array of update results
 */
function restoreVersions(updates) {
  updates.forEach(update => {
    if (update.success && update.isSnapshot && update.path) {
      try {
        const packageJson = JSON.parse(fs.readFileSync(update.path, 'utf8'));
        packageJson.version = update.originalVersion;
        fs.writeFileSync(update.path, JSON.stringify(packageJson, null, 2) + '\n');
      } catch (error) {
        console.error(`Error restoring ${update.path}: ${error.message}`);
      }
    }
  });
}

// Main execution
const packageJsonFiles = findPackageJsonFiles(baseDirectory);

if (packageJsonFiles.length === 0) {
  process.exit(1);
}

// Generate a common timestamp for all packages.
const timestamp = Math.floor(Date.now() / 1000).toString();

// Determine if we're dealing with SNAPSHOT or release versions
// Check the first package.json to determine the mode
let isSnapshotMode = false;
try {
  // Look for the first package.json file with a version.
  const firstVersion = packageJsonFiles.map(packageJsonPath => JSON.parse(fs.readFileSync(packageJsonPath, 'utf8')))
    .filter(packageJson => packageJson.version)
    .map(packageJson => packageJson.version)[0];
  isSnapshotMode = firstVersion.includes('SNAPSHOT');
} catch (error) {
  console.error('Error determining version mode');
  process.exit(1);
}

// Step 1: Update all package.json versions
const updates = packageJsonFiles
  .map(packageJsonPath => {
    let packageJson;

    try {
      packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    } catch (error) {
      console.error(`Error reading ${packageJsonPath}: ${error.message}`);
      return {success: false};
    }

    return {packageJsonPath, packageJson, success: true}
  })
  .filter(({packageJsonPath, packageJson, success}) => success && packageJson.version)
  .map(({packageJsonPath, packageJson}) => {
  const result = updatePackageVersion(packageJsonPath, packageJson, timestamp);
  return {...result, path: packageJsonPath};
});

const failedUpdates = updates.filter(u => !u.success);
if (failedUpdates.length > 0) {
  console.error(`Failed to update ${failedUpdates.length} package(s)`);
  console.error('summary', updates);
  process.exit(1);
}

try {
  if (isSnapshotMode) {
    execSync(
      `pnpm -r publish --registry ${snapshotRegistry} --tag snapshot --no-git-checks`,
      { stdio: 'inherit', cwd: baseDirectory }
    );
  } else {
    execSync(
      `pnpm -r publish --registry ${releaseRegistry} --access public`,
      { stdio: 'inherit', cwd: baseDirectory }
    );
  }
} catch (error) {
  console.error('Error during publication', error.message);
  process.exit(1);
} finally {
  // Restore versions before exiting
  if (isSnapshotMode) {
    restoreVersions(updates);
  }
}

