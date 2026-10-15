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

import {compareVersions, isSnapshot, parseVersion} from './version.js';

export const SNAPSHOT_TAG = 'snapshot';

export const RELEASE_CANDIDATE_TAG = 'rc';

export const LATEST_TAG = 'latest';

/**
 * The npm dist-tag a publication writes to. It is never "latest": npm always writes the tag passed
 * to the publish command, so publishing a maintenance branch under "latest" would make it point to
 * an older version than the one already published. "latest" is instead moved afterwards, and only
 * forward, by planLatestMoves().
 *
 * @param {string} version the version being published
 * @returns {string} the dist-tag to publish under
 */
export function computePublishTag(version) {
  if (isSnapshot(version)) {
    return SNAPSHOT_TAG;
  }

  const {major, minor, rc} = parseVersion(version);
  if (rc !== null) {
    return RELEASE_CANDIDATE_TAG;
  }

  // Named after the git branch the version is released from, so that a maintenance line stays
  // installable explicitly, e.g. "npm install @xwiki/platform-api@stable-18.4.x".
  return `stable-${major}.${minor}.x`;
}

/**
 * Decide the "latest" dist-tag of a single package.
 *
 * @param {string|null} currentLatest the version "latest" currently points to, null when the
 *   package has no "latest" tag yet (it has never been published, or was only ever published under
 *   another tag)
 * @param {string} version the version being published
 * @returns {boolean} true when "latest" must be moved to the published version
 * @throws {Error} when either version cannot be parsed, leaving the decision undecidable
 */
export function shouldMoveLatest(currentLatest, version) {
  // Without a "latest" tag "npm install <package>" fails, so bootstrap it whatever the version is.
  if (currentLatest === null || currentLatest === undefined) {
    return true;
  }

  // A release candidate never becomes the default version users install.
  if (parseVersion(version).rc !== null) {
    return false;
  }

  return compareVersions(version, currentLatest) > 0;
}

/**
 * Compute the "latest" dist-tag updates to apply after a publication, from the state of the
 * registry.
 *
 * @param {{packages: {name: string}[], distTags: Object<string, Object<string, string>|null>,
 *   version: string}} state the published packages, the dist-tags each of them currently has on
 *   the registry (null when the package is unknown to the registry), and the published version
 * @returns {{moves: {name: string, version: string}[], undecidable: {name: string,
 *   currentLatest: string, reason: string}[]}} the packages whose "latest" must be moved, and the
 *   ones whose current "latest" could not be compared
 */
export function planLatestMoves({packages, distTags, version}) {
  const moves = [];
  const undecidable = [];

  for (const {name} of packages) {
    const tags = distTags[name];
    const currentLatest = tags ? tags[LATEST_TAG] ?? null : null;

    try {
      if (shouldMoveLatest(currentLatest, version)) {
        moves.push({name, version});
      }
    } catch (error) {
      undecidable.push({name, currentLatest, reason: error.message});
    }
  }

  return {moves, undecidable};
}
