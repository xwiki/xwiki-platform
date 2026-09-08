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
import {computePublishTag, LATEST_TAG, planLatestMoves} from './lib/tags.js';
import {addDistTag, distTagArguments, readDistTags, resolveNpmBinary} from './lib/registry.js';

// Allow skipping the npm publication process. This is useful the case where the maven
// release failed after the npm publication process passed successfully as we want to
// avoid republishing the npm packages again (it would lead to errors).
if (process.env.SKIP_NPM_PUBLICATION === "true") {
  process.exit(0);
}

// Print the publication commands instead of running them, to review what a release would do. The
// registry is still read, so that the printed plan is the one that would really be applied.
const dryRun = process.env.DEPLOY_DRY_RUN === "true";

/**
 * Script to conditionally publish npm packages based on version type (SNAPSHOT vs release)
 * Step 1: Updates all package.json files with the same timestamp
 * Step 2: Uses pnpm -r publish to publish all packages
 * Step 3: For a release, moves the "latest" dist-tag to the published version, but only for the
 *         packages where it points to an older version
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

/**
 * Move the "latest" dist-tag of the published packages to the published version, for the packages
 * where it points to an older version, or where it does not exist yet.
 *
 * "latest" is what "npm install <package>" resolves to, so it must always point to the newest
 * released version. Since releases are not published in version order (a maintenance branch is
 * released after a newer branch, and a release candidate is published before the final version),
 * the dist-tag the publication writes is never "latest", and this decides afterwards whether
 * "latest" can move to it.
 *
 * The packages are already published when this runs, so a failure here never fails the build: it
 * is reported with the commands needed to fix it, and any later release fixes it on its own.
 *
 * @param {{name: string}[]} packages the published packages
 * @param {string} version the published version
 * @returns {Promise<void>} resolved once every dist-tag has been dealt with
 */
async function moveLatestForward(packages, version) {
  const npmBinary = resolveNpmBinary();
  const names = packages.map(({name}) => name);

  console.log(`Reading the "${LATEST_TAG}" dist-tag of ${names.length} package(s) from ${releaseRegistry}`);
  const {distTags, failures} = await readDistTags(names, {registry: releaseRegistry, npmBinary});

  // A package whose dist-tags could not be read is left alone: not knowing where "latest" points
  // is not the same as it not existing, and moving it blindly could make it go backwards.
  const failedNames = new Set(failures.map(({name}) => name));
  const readPackages = packages.filter(({name}) => !failedNames.has(name));

  const {moves, undecidable} = planLatestMoves({packages: readPackages, distTags, version});
  const unchanged = readPackages.length - moves.length - undecidable.length;
  console.log(`"${LATEST_TAG}" moves to ${version} for ${moves.length} package(s), ` +
    `${unchanged} package(s) already point to ${version} or to a newer version`);

  const writeFailures = [];
  for (const move of moves) {
    const target = {...move, tag: LATEST_TAG, registry: releaseRegistry};

    if (dryRun) {
      console.log(`[dry run] ${npmBinary} ${distTagArguments(target).join(' ')}`);
      continue;
    }

    try {
      await addDistTag({...target, npmBinary});
      console.log(`Set "${LATEST_TAG}" of ${move.name} to ${move.version}`);
    } catch (error) {
      writeFailures.push({...target, message: error.message});
    }
  }

  reportDistTagProblems({failures, undecidable, writeFailures, npmBinary, version});
}

/**
 * Report, as a single block, every package whose "latest" dist-tag could not be dealt with, with
 * the command to run to fix it.
 *
 * @param {{failures: Object[], undecidable: Object[], writeFailures: Object[], npmBinary: string,
 *   version: string}} problems the read failures, the packages whose current "latest" could not be
 *   compared, the write failures, the npm executable and the published version
 */
function reportDistTagProblems({failures, undecidable, writeFailures, npmBinary, version}) {
  if (failures.length === 0 && undecidable.length === 0 && writeFailures.length === 0) {
    return;
  }

  console.error('');
  console.error('========================================================================');
  console.error(`The "${LATEST_TAG}" dist-tag could not be updated for ` +
    `${failures.length + undecidable.length + writeFailures.length} package(s).`);
  console.error(`The packages themselves are published: only "${LATEST_TAG}" is left untouched, ` +
    'which means it may still point to an older version.');
  console.error('');

  failures.forEach(({name, message}) =>
    console.error(`Failed to read the dist-tags of ${name}: ${message}`));
  undecidable.forEach(({name, currentLatest, reason}) =>
    console.error(`Cannot compare ${name}@${currentLatest} with ${version}: ${reason}`));
  writeFailures.forEach(({name, message}) =>
    console.error(`Failed to set the dist-tag of ${name}: ${message}`));

  console.error('');
  console.error('Run the following command(s) once the cause is fixed:');
  [...failures, ...undecidable, ...writeFailures].forEach(({name}) => console.error(
    `  ${npmBinary} ${distTagArguments({name, version, tag: LATEST_TAG, registry: releaseRegistry}).join(' ')}`));
  console.error('========================================================================');
  console.error('');
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
  .map(({packageName, newVersion, originalVersion}) => ({
    name: packageName,
    version: newVersion,
    originalVersion,
  }));

const publishedVersions = [...new Set(publishedPackages.map(({version}) => version))];
const declaredVersions = [...new Set(publishedPackages.map(({originalVersion}) => originalVersion))];
if (publishedVersions.length !== 1 || declaredVersions.length !== 1) {
  console.error(`Expected a single version to publish but found: ${declaredVersions.join(', ')}`);
  process.exit(1);
}
const publishedVersion = publishedVersions[0];
// The dist-tag is computed from the version declared in the package.json files rather than from the
// published one, because a SNAPSHOT version has already been replaced by a timestamp at this point.
const publishTag = computePublishTag(declaredVersions[0]);

try {
  const registry = isSnapshotMode ? snapshotRegistry : releaseRegistry;
  const access = isSnapshotMode ? '' : ' --access public';
  const command = `pnpm -r publish --registry ${registry}${access} --tag ${publishTag} --no-git-checks`;

  if (dryRun) {
    console.log(`[dry run] ${command}`);
  } else {
    execSync(command, { stdio: 'inherit', cwd: baseDirectory });
  }

  if (!isSnapshotMode) {
    // Step 3: a release publishes under its own dist-tag, so "latest" is moved separately.
    await moveLatestForward(publishedPackages, publishedVersion);
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
