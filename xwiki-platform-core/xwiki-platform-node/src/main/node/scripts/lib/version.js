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

/**
 * The version format used by every XWiki release: Major.Minor.Bugfix, optionally followed by a
 * release candidate number. Anything else is rejected rather than guessed at, so that an
 * unexpected version can never be mis-ordered silently.
 */
const RELEASE_VERSION_PATTERN = /^(\d+)\.(\d+)\.(\d+)(?:-rc-(\d+))?$/;

/**
 * @param {string} version a version string
 * @returns {boolean} true when the version is a development (SNAPSHOT) version
 */
export function isSnapshot(version) {
  return version.includes('SNAPSHOT');
}

/**
 * @param {string} version a released version, e.g. "18.4.5" or "18.7.0-rc-1"
 * @returns {{major: number, minor: number, bugfix: number, rc: number|null}} its parts
 * @throws {Error} when the version does not follow the XWiki release version format
 */
export function parseVersion(version) {
  const parts = RELEASE_VERSION_PATTERN.exec(version);
  if (!parts) {
    throw new Error(`Unsupported version format: [${version}]`);
  }

  return {
    major: Number(parts[1]),
    minor: Number(parts[2]),
    bugfix: Number(parts[3]),
    rc: parts[4] === undefined ? null : Number(parts[4]),
  };
}

/**
 * @param {string} version a released version
 * @returns {boolean} true when the version is a release candidate
 */
export function isPrerelease(version) {
  return parseVersion(version).rc !== null;
}

/**
 * Order two released versions. A final version is greater than any of its release candidates.
 *
 * @param {string} left a released version
 * @param {string} right a released version
 * @returns {number} a negative number when left is older than right, 0 when they are equal, a
 *   positive number when left is newer than right
 */
export function compareVersions(left, right) {
  const a = parseVersion(left);
  const b = parseVersion(right);

  for (const part of ['major', 'minor', 'bugfix']) {
    if (a[part] !== b[part]) {
      return a[part] - b[part];
    }
  }

  if (a.rc === b.rc) {
    return 0;
  }
  // A final version has no rc number and is newer than any release candidate of the same version.
  if (a.rc === null) {
    return 1;
  }
  if (b.rc === null) {
    return -1;
  }

  return a.rc - b.rc;
}
