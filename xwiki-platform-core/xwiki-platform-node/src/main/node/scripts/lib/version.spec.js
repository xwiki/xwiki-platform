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
import {compareVersions, isPrerelease, isSnapshot, parseVersion} from './version.js';

describe('isSnapshot', () => {
  it('recognizes development versions', () => {
    expect(isSnapshot('18.8.0-SNAPSHOT')).toBe(true);
    expect(isSnapshot('18.4.5')).toBe(false);
    expect(isSnapshot('18.7.0-rc-1')).toBe(false);
  });
});

describe('parseVersion', () => {
  it('parses a final version', () => {
    expect(parseVersion('18.4.5')).toEqual({major: 18, minor: 4, bugfix: 5, rc: null});
  });

  it('parses a release candidate', () => {
    expect(parseVersion('18.7.0-rc-2')).toEqual({major: 18, minor: 7, bugfix: 0, rc: 2});
  });

  it.each(['18.7', '18.7.0-SNAPSHOT', '18.7.0-1757312345', '18.7.0.1', 'v18.7.0', ''])(
    'rejects [%s] rather than guessing its order', version => {
      expect(() => parseVersion(version)).toThrow(`Unsupported version format: [${version}]`);
    });
});

describe('isPrerelease', () => {
  it('distinguishes release candidates from final versions', () => {
    expect(isPrerelease('18.7.0-rc-1')).toBe(true);
    expect(isPrerelease('18.7.0')).toBe(false);
  });
});

describe('compareVersions', () => {
  it.each([
    ['18.4.5', '18.7.0'],
    ['18.4.0', '18.10.0'],
    ['18.4.5', '19.0.0'],
    ['18.4.4', '18.4.5'],
    ['18.7.0-rc-1', '18.7.0'],
    ['18.7.0-rc-1', '18.7.0-rc-2'],
    ['18.7.0', '18.7.1-rc-1'],
  ])('orders %s before %s', (older, newer) => {
    expect(compareVersions(older, newer)).toBeLessThan(0);
    expect(compareVersions(newer, older)).toBeGreaterThan(0);
  });

  it.each(['18.4.5', '18.7.0-rc-1'])('considers %s equal to itself', version => {
    expect(compareVersions(version, version)).toBe(0);
  });

  it('compares the minor numerically and not lexically', () => {
    // "18.10.0" sorts before "18.4.0" as a string.
    expect(compareVersions('18.10.0', '18.4.0')).toBeGreaterThan(0);
  });
});
