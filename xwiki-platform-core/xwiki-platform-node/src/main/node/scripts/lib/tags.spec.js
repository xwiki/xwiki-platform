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

import {describe, expect, it} from 'vitest';
import {computeDistTag, maintenanceTag, needsPublishedVersions} from './tags.js';

// The final and release candidate versions on the registry once 18.8.0 and 18.4.7 are released.
const PUBLISHED = ['18.4.0-rc-1', '18.4.0', '18.4.6', '18.8.0-rc-1', '18.8.0', '18.4.7'];

describe('needsPublishedVersions', () => {
  it('only reads the registry for a final release', () => {
    expect(needsPublishedVersions('18.10.0-SNAPSHOT')).toBe(false);
    expect(needsPublishedVersions('18.10.0-rc-1')).toBe(false);
    expect(needsPublishedVersions('18.10.0')).toBe(true);
  });
});

describe('maintenanceTag', () => {
  it.each([
    ['18.4.8', 'stable-18.4.x'],
    ['18.10.0', 'stable-18.10.x'],
  ])('names the maintenance line of %s "%s"', (version, tag) => {
    expect(maintenanceTag(version)).toBe(tag);
  });
});

describe('computeDistTag', () => {
  it('publishes a SNAPSHOT version under "snapshot"', () => {
    expect(computeDistTag('18.10.0-SNAPSHOT')).toBe('snapshot');
  });

  it.each(['18.10.0-rc-1', '18.10.0-rc-2', '19.0.0-rc-1'])('publishes the release candidate %s under "rc"',
    version => {
      expect(computeDistTag(version, PUBLISHED)).toBe('rc');
    });

  it.each(['18.8.1', '18.9.0', '18.10.0', '19.0.0'])(
    'publishes %s under "latest" since it is higher than every final release', version => {
      expect(computeDistTag(version, PUBLISHED)).toBe('latest');
    });

  it('publishes a final release under "latest" when only a release candidate is higher', () => {
    expect(computeDistTag('18.8.1', [...PUBLISHED, '18.9.0-rc-1'])).toBe('latest');
  });

  it('publishes a final release under "latest" when the package has never been published', () => {
    expect(computeDistTag('18.4.8', [])).toBe('latest');
  });

  it.each([
    ['18.4.8', 'stable-18.4.x'],
    ['18.8.0', 'stable-18.8.x'],
  ])('publishes %s under "%s" since a higher final release exists', (version, tag) => {
    expect(computeDistTag(version, [...PUBLISHED, '18.9.0'])).toBe(tag);
  });

  it('compares the minor numerically and not lexically', () => {
    // "18.10.0" sorts before "18.9.0" as a string.
    expect(computeDistTag('18.9.1', ['18.9.0', '18.10.0'])).toBe('stable-18.9.x');
    expect(computeDistTag('18.10.1', ['18.9.0', '18.10.0'])).toBe('latest');
  });

  it('ignores the version being published, so that a re-run after a partial failure keeps "latest"', () => {
    // The first run published some packages of 18.10.0 before failing.
    expect(computeDistTag('18.10.0', [...PUBLISHED, '18.10.0'])).toBe('latest');
    // The first run of a maintenance release published some packages of 18.4.8 before failing.
    expect(computeDistTag('18.4.8', [...PUBLISHED, '18.4.8'])).toBe('stable-18.4.x');
  });

  it.each(['18.4.8', '18.10.0'])('computes a dist-tag name npm accepts for %s', version => {
    // npm rejects a dist-tag name that is a valid semver range, e.g. "18.4" or "v18.4".
    expect(computeDistTag(version, PUBLISHED)).toMatch(/^(latest|stable-\d+\.\d+\.x)$/);
  });

  it('ignores the published versions that are not final releases', () => {
    expect(computeDistTag('18.9.1', [...PUBLISHED, '18.10.0-milestone-1', 'not-a-version'])).toBe('latest');
  });

  it('fails on an unexpected version format', () => {
    expect(() => computeDistTag('18.10', PUBLISHED)).toThrow('Unsupported version format: [18.10]');
  });
});
