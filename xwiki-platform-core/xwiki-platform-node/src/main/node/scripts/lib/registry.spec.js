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

import {describe, expect, it, vi} from 'vitest';
import {packageUrl, readPublishedVersions} from './registry.js';

const REGISTRY = 'https://registry.npmjs.org';

/**
 * @param {Object<string, number|string[]>} packages for each package URL, the versions the
 *   registry answers with, or the HTTP status of its error
 * @returns {function} a fetch function answering like the registry
 */
function registryFetch(packages) {
  return vi.fn(async url => {
    const answer = packages[url] ?? 404;
    if (typeof answer === 'number') {
      return {ok: false, status: answer};
    }
    const versions = Object.fromEntries(answer.map(version => [version, {}]));
    return {ok: true, status: 200, json: async () => ({name: url, versions})};
  });
}

describe('packageUrl', () => {
  it('encodes the slash of a scoped package name', () => {
    expect(packageUrl(REGISTRY, '@xwiki/platform-api')).toBe(`${REGISTRY}/@xwiki%2fplatform-api`);
    expect(packageUrl(`${REGISTRY}/`, '@xwiki/platform-api')).toBe(`${REGISTRY}/@xwiki%2fplatform-api`);
  });
});

describe('readPublishedVersions', () => {
  it('reads the versions of the first package the registry knows', async () => {
    const fetch = registryFetch({
      [`${REGISTRY}/@xwiki%2fb`]: ['18.4.0', '18.8.0'],
      [`${REGISTRY}/@xwiki%2fc`]: ['18.4.0'],
    });

    const versions = await readPublishedVersions(['@xwiki/a', '@xwiki/b', '@xwiki/c'], {registry: REGISTRY, fetch});

    expect(versions).toEqual(['18.4.0', '18.8.0']);
    // "a" has never been published, "c" is not needed.
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(fetch).toHaveBeenCalledWith(`${REGISTRY}/@xwiki%2fa`,
      {headers: {accept: 'application/vnd.npm.install-v1+json'}});
  });

  it('answers no version when the registry knows none of the packages', async () => {
    const versions = await readPublishedVersions(['@xwiki/a', '@xwiki/b'], {registry: REGISTRY,
      fetch: registryFetch({})});

    expect(versions).toEqual([]);
  });

  it('fails when the registry answers with an error', async () => {
    const fetch = registryFetch({[`${REGISTRY}/@xwiki%2fa`]: 503});

    await expect(readPublishedVersions(['@xwiki/a', '@xwiki/b'], {registry: REGISTRY, fetch}))
      .rejects.toThrow(`Failed to query [${REGISTRY}/@xwiki%2fa]: HTTP 503`);
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('fails when the registry cannot be reached', async () => {
    // Node's fetch reports the network error as the cause of a generic one.
    const fetch = vi.fn(async () => {
      throw new TypeError('fetch failed', {cause: {code: 'ENOTFOUND'}});
    });

    await expect(readPublishedVersions(['@xwiki/a'], {registry: REGISTRY, fetch}))
      .rejects.toThrow(`Failed to query [${REGISTRY}/@xwiki%2fa]: fetch failed (ENOTFOUND)`);
  });
});
