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
 * Build the "pnpm -r publish" command of a publication.
 * @param {Object} options the publication options
 * @param {string} options.registry the registry the packages are published to
 * @param {boolean} options.snapshot true when publishing SNAPSHOT versions, which go to a private registry and
 *   need no "--access" option
 * @param {string} options.distTag the dist-tag the packages are published under, see computeDistTag()
 * @param {boolean} options.dryRun true to only check that the packages can be published: pnpm runs every
 *   check of a publication but sends nothing to the registry
 * @returns {string} the command to run from the workspace root
 */
export function publishCommand({registry, snapshot, distTag, dryRun}) {
  const access = snapshot ? '' : ' --access public';
  const dryRunOption = dryRun ? ' --dry-run' : '';
  return `pnpm -r publish --registry ${registry}${access} --tag ${distTag} --no-git-checks${dryRunOption}`;
}
