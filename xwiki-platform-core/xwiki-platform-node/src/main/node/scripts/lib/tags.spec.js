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
import {computePublishTag, LATEST_TAG, planLatestMoves, shouldMoveLatest} from './tags.js';

describe('computePublishTag', () => {
  it.each([
    ['18.8.0-SNAPSHOT', 'snapshot'],
    ['18.7.0-rc-1', 'rc'],
    ['18.7.0-rc-2', 'rc'],
    ['18.4.5', 'stable-18.4.x'],
    ['18.10.0', 'stable-18.10.x'],
    ['19.0.0', 'stable-19.0.x'],
  ])('publishes %s under the "%s" dist-tag', (version, tag) => {
    expect(computePublishTag(version)).toBe(tag);
  });

  it('never publishes under "latest", which is moved separately', () => {
    expect(computePublishTag('18.4.5')).not.toBe(LATEST_TAG);
  });

  it.each(['18.8.0-SNAPSHOT', '18.7.0-rc-1', '18.4.5'])(
    'computes a tag name npm accepts for %s', version => {
      // npm rejects a dist-tag name that is a valid SemVer range, e.g. "18.4" or "v18.4".
      expect(computePublishTag(version)).toMatch(/^[a-z][a-z-]*(-\d+\.\d+\.x)?$/);
    });
});

describe('shouldMoveLatest', () => {
  it('moves "latest" forward', () => {
    expect(shouldMoveLatest('18.4.4', '18.4.5')).toBe(true);
    expect(shouldMoveLatest('18.7.0-rc-1', '18.7.0')).toBe(true);
  });

  it('never moves "latest" backwards', () => {
    expect(shouldMoveLatest('18.7.0', '18.4.5')).toBe(false);
  });

  it('does nothing when "latest" already points to the published version', () => {
    expect(shouldMoveLatest('18.4.5', '18.4.5')).toBe(false);
  });

  it('never points "latest" to a release candidate of an existing package', () => {
    expect(shouldMoveLatest('18.4.5', '18.8.0-rc-1')).toBe(false);
  });

  it.each([null, undefined])('bootstraps a package that has no "latest" yet (%s)', currentLatest => {
    // Without a "latest" tag "npm install <package>" fails, even for a release candidate.
    expect(shouldMoveLatest(currentLatest, '18.4.5')).toBe(true);
    expect(shouldMoveLatest(currentLatest, '18.8.0-rc-1')).toBe(true);
  });

  it('refuses to decide when a version cannot be parsed', () => {
    expect(() => shouldMoveLatest('18.4.5-1757312345', '18.4.6')).toThrow('Unsupported version format');
  });
});

describe('planLatestMoves', () => {
  const packages = [{name: 'older'}, {name: 'equal'}, {name: 'newer'}, {name: 'unknown'}];
  const distTags = {
    older: {latest: '18.4.4'},
    equal: {latest: '18.4.5'},
    newer: {latest: '18.7.0'},
    unknown: null,
  };

  it('only moves the packages whose "latest" is older or missing', () => {
    expect(planLatestMoves({packages, distTags, version: '18.4.5'})).toEqual({
      moves: [{name: 'older', version: '18.4.5'}, {name: 'unknown', version: '18.4.5'}],
      undecidable: [],
    });
  });

  it('moves nothing but the unknown package for a release candidate', () => {
    expect(planLatestMoves({packages, distTags, version: '18.8.0-rc-1'})).toEqual({
      moves: [{name: 'unknown', version: '18.8.0-rc-1'}],
      undecidable: [],
    });
  });

  it('treats a package published without any dist-tag as needing "latest"', () => {
    expect(planLatestMoves({packages: [{name: 'tagless'}], distTags: {tagless: {}}, version: '18.4.5'}))
      .toEqual({moves: [{name: 'tagless', version: '18.4.5'}], undecidable: []});
  });

  it('reports the packages whose current "latest" cannot be compared, and moves the others', () => {
    const withUnparseable = {...distTags, weird: {latest: 'not-a-version'}};

    expect(planLatestMoves({
      packages: [...packages, {name: 'weird'}],
      distTags: withUnparseable,
      version: '18.4.5',
    })).toEqual({
      moves: [{name: 'older', version: '18.4.5'}, {name: 'unknown', version: '18.4.5'}],
      undecidable: [{
        name: 'weird',
        currentLatest: 'not-a-version',
        reason: 'Unsupported version format: [not-a-version]',
      }],
    });
  });
});
