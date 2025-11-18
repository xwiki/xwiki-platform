#!/usr/bin/env node

const fs = require('fs');
const { execSync } = require('child_process');
const path = require('path');

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
 * @param {string} timestamp - Timestamp to replace SNAPSHOT with
 * @returns {Object} - {success, originalVersion, newVersion, packageName, isSnapshot}
 */
function updatePackageVersion(packageJsonPath, timestamp) {
  let packageJson;

  try {
    packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
  } catch (error) {
    console.error(`Error reading ${packageJsonPath}: ${error.message}`);
    return { success: false };
  }

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
      console.error(`Error writing ${packageJsonPath}: ${error.message}`);
      return { success: false };
    }
  } else {
    console.log(`  • ${packageName}: ${originalVersion} (release version, no change)`);
    return { success: true, originalVersion, newVersion: originalVersion, packageName, isSnapshot: false };
  }
}

/**
 * Restore original versions in all package.json files
 * @param {Array} updates - Array of update results
 */
function restoreVersions(updates) {
  console.log('\nRestoring original versions...');
  updates.forEach(update => {
    if (update.success && update.isSnapshot && update.path) {
      try {
        const packageJson = JSON.parse(fs.readFileSync(update.path, 'utf8'));
        packageJson.version = update.originalVersion;
        fs.writeFileSync(update.path, JSON.stringify(packageJson, null, 2) + '\n');
        console.log(`  ✓ Restored ${update.packageName} to ${update.originalVersion}`);
      } catch (error) {
        console.error(`  ✗ Error restoring ${update.path}: ${error.message}`);
      }
    }
  });
}

// Main execution
console.log(`Searching for package.json files in: ${baseDirectory}\n`);
const packageJsonFiles = findPackageJsonFiles(baseDirectory);

console.log(`Found ${packageJsonFiles.length} package.json file(s)\n`);

if (packageJsonFiles.length === 0) {
  console.error('No package.json files found!');
  process.exit(1);
}

// Generate single timestamp for all packages
const timestamp = Math.floor(Date.now() / 1000);
console.log(`Using timestamp: ${timestamp}\n`);

// Determine if we're dealing with SNAPSHOT or release versions
// Check the first package.json to determine the mode
let isSnapshotMode = false;
try {
  const firstPackage = JSON.parse(fs.readFileSync(packageJsonFiles[0], 'utf8'));
  isSnapshotMode = firstPackage.version.includes('SNAPSHOT');
} catch (error) {
  console.error('Error determining version mode');
  process.exit(1);
}

console.log(`Mode: ${isSnapshotMode ? 'SNAPSHOT' : 'RELEASE'}\n`);

// Step 1: Update all package.json versions
console.log('='.repeat(80));
console.log('STEP 1: Updating package.json versions');
console.log('='.repeat(80));

const updates = packageJsonFiles.map(packageJsonPath => {
  const result = updatePackageVersion(packageJsonPath, timestamp.toString());
  return { ...result, path: packageJsonPath };
});

const failedUpdates = updates.filter(u => !u.success);
if (failedUpdates.length > 0) {
  console.error(`\n✗ Failed to update ${failedUpdates.length} package(s)`);
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

