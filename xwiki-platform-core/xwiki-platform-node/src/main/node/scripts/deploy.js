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
import {computeDistTag, needsPublishedVersions} from './lib/tags.js';
import {readPublishedVersions} from './lib/registry.js';
import {publishCommand} from './lib/publish.js';

// Run every check of the publication without sending anything to the registry. The release script
// sets it for the Maven release, then publishes the release itself once the release manager is
// there to approve the publication with their two-factor authentication.
const dryRun = process.env.XWIKI_NPM_PUBLISH_DRY_RUN === "true";

/**
 * Script to conditionally publish npm packages based on version type (SNAPSHOT vs release)
 * Step 1: Updates all package.json files with the same timestamp
 * Step 2: Computes the dist-tag of the version, see computeDistTag()
 * Step 3: Uses pnpm -r publish to publish all packages under that dist-tag
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
 * @returns {Object} - {success, originalVersion, newVersion, packageName, isSnapshot, isPrivate}
 */
function updatePackageVersion(packageJsonPath, packageJson, timestamp) {
  const originalVersion = packageJson.version;
  const packageName = packageJson.name;
  const isSnapshot = originalVersion.includes('SNAPSHOT');
  // A private package is never sent to a registry by "pnpm publish".
  const isPrivate = packageJson.private === true;

  if (isSnapshot) {
    // Replace SNAPSHOT with timestamp
    const newVersion = originalVersion.replace('SNAPSHOT', timestamp);
    packageJson.version = newVersion;

    try {
      fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2) + '\n');
      return { success: true, originalVersion, newVersion, packageName, isSnapshot: true, isPrivate };
    } catch (error) {
      return { success: false };
    }
  } else {
    return { success: true, originalVersion, newVersion: originalVersion, packageName, isSnapshot: false, isPrivate };
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

// The packages "pnpm publish" sends to the registry.
const publishedPackages = updates
  .filter(({isPrivate, packageName}) => !isPrivate && packageName)
  .map(({packageName, originalVersion}) => ({name: packageName, originalVersion}));

const declaredVersions = [...new Set(publishedPackages.map(({originalVersion}) => originalVersion))];
if (declaredVersions.length !== 1) {
  console.error(`Expected a single version to publish but found: ${declaredVersions.join(', ')}`);
  restoreVersions(updates);
  process.exit(1);
}
// The dist-tag is computed from the version declared in the package.json files rather than from the
// published one, because a SNAPSHOT version has already been replaced by a timestamp at this point.
const declaredVersion = declaredVersions[0];

let distTag;
try {
  const existingVersions = needsPublishedVersions(declaredVersion)
    ? await readPublishedVersions(publishedPackages.map(({name}) => name), {registry: releaseRegistry})
    : [];
  distTag = computeDistTag(declaredVersion, existingVersions);
  console.log(`Publishing ${declaredVersion} under the "${distTag}" dist-tag`);
} catch (error) {
  console.error('Error while computing the dist-tag', error.message);
  restoreVersions(updates);
  process.exit(1);
}

try {
  const registry = isSnapshotMode ? snapshotRegistry : releaseRegistry;
  const command = publishCommand({registry, snapshot: isSnapshotMode, distTag, dryRun});
  execSync(command, { stdio: 'inherit', cwd: baseDirectory });
} catch (error) {
  console.error('Error during publication', error.message);
  process.exit(1);
} finally {
  // Restore versions before exiting
  if (isSnapshotMode) {
    restoreVersions(updates);
  }
}
