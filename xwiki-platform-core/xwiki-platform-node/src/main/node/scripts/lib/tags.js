/*
 * See the NOTICE file distributed with this work for additional
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

import semver from 'semver';

export const SNAPSHOT_TAG = 'snapshot';

export const RELEASE_CANDIDATE_TAG = 'rc';

export const LATEST_TAG = 'latest';

/**
 * @param {string} version a final release version, e.g. "18.4.8"
 * @returns {string} the dist-tag of a maintenance release of that version's line, e.g. "stable-18.4.x"
 */
export function maintenanceTag(version) {
  return `stable-${semver.major(version)}.${semver.minor(version)}.x`;
}

/**
 * @param {string} version the version being published
 * @returns {boolean} true when the dist-tag of the version depends on the versions already on the
 *   registry, i.e. when the version is a final release
 */
export function needsPublishedVersions(version) {
  return !version.includes('SNAPSHOT') && !semver.prerelease(version);
}

/**
 * Compute the single npm dist-tag a version is published under. The dist-tag only depends on the
 * version and on the versions already on the registry: the release manager has no input and cannot
 * override it.
 *
 * - a SNAPSHOT version gets "snapshot"
 * - a release candidate gets "rc"
 * - a final release gets "latest" when it is higher than every final release already on the
 *   registry, or when the registry has none
 * - any other final release gets maintenanceTag(), e.g. "stable-18.4.x". npm requires a dist-tag on
 *   every publication, and "pnpm publish" writes "latest" when given none, so a maintenance release
 *   needs a dist-tag of its own to leave "latest" untouched. Consumers are not expected to use it:
 *   they select a maintenance line with a semver range, e.g. "~18.4.0". It cannot be named "18.4" or
 *   "v18.4", because npm rejects a dist-tag name that is a valid semver range.
 *
 * @param {string} version the version being published
 * @param {string[]} publishedVersions the versions already on the registry, empty when the package
 *   has never been published. Only used for a final release.
 * @returns {string} the dist-tag to publish under
 * @throws {Error} when the version is not a valid semver version
 */
export function computeDistTag(version, publishedVersions = []) {
  if (version.includes('SNAPSHOT')) {
    return SNAPSHOT_TAG;
  }
  if (!semver.valid(version)) {
    throw new Error(`Unsupported version format: [${version}]`);
  }
  if (semver.prerelease(version)) {
    return RELEASE_CANDIDATE_TAG;
  }

  // The "*" range only matches final releases, so the release candidates on the registry are ignored.
  // The version being published is excluded, so that re-running a publication that failed midway
  // still resolves to the same dist-tag once part of the packages is on the registry.
  const highestFinal = semver.maxSatisfying(publishedVersions.filter(published => published !== version), '*');
  if (highestFinal === null || semver.gt(version, highestFinal)) {
    return LATEST_TAG;
  }

  return maintenanceTag(version);
}
