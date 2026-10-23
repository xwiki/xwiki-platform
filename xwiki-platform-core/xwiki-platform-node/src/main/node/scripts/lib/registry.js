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
 * @param {string} registry the registry URL, e.g. "https://registry.npmjs.org"
 * @param {string} name the package name, e.g. "@xwiki/platform-api"
 * @returns {string} the URL of the package metadata on the registry
 */
export function packageUrl(registry, name) {
  // The registry expects the slash of a scoped package name to be encoded.
  return `${registry.replace(/\/+$/, '')}/${name.replace('/', '%2f')}`;
}

/**
 * Read the versions already published on a registry, from the first of the given packages the
 * registry knows. The packages of a release are all released together with the same version, so
 * any of them tells which versions were released.
 *
 * @param {string[]} names the names of the packages being published, in the order to look them up
 * @param {{registry: string, fetch?: function}} options the registry to read from and the
 *   injectable fetch function
 * @returns {Promise<string[]>} the published versions of the first package found on the registry,
 *   empty when the registry knows none of the packages
 * @throws {Error} when the registry cannot be queried, since the dist-tag would then be guessed
 */
export async function readPublishedVersions(names, options) {
  const {registry, fetch = globalThis.fetch} = options;

  for (const name of names) {
    const url = packageUrl(registry, name);
    let response;
    try {
      // The abbreviated metadata is enough to list the versions and much smaller than the full one.
      response = await fetch(url, {headers: {accept: 'application/vnd.npm.install-v1+json'}});
    } catch (error) {
      // Node's fetch hides the network error (DNS, refused connection...) in the cause.
      const reason = error.cause ? `${error.message} (${error.cause.code ?? error.cause.message})` : error.message;
      throw new Error(`Failed to query [${url}]: ${reason}`, {cause: error});
    }

    if (response.status === 404) {
      // This package has never been published, look for another one.
      continue;
    }
    if (!response.ok) {
      throw new Error(`Failed to query [${url}]: HTTP ${response.status}`);
    }

    const {versions = {}} = await response.json();
    return Object.keys(versions);
  }

  return [];
}
