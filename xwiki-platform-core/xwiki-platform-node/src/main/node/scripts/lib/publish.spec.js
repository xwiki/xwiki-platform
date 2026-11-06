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
import {publishCommand} from './publish.js';

const RELEASE = {registry: 'https://registry.npmjs.org/', snapshot: false, distTag: 'latest'};

describe('publishCommand', () => {
  it('publishes a release publicly', () => {
    expect(publishCommand({...RELEASE, dryRun: false})).toBe(
      'pnpm -r publish --registry https://registry.npmjs.org/ --access public --tag latest --no-git-checks');
  });

  it('publishes a SNAPSHOT without an access level', () => {
    expect(publishCommand({registry: 'https://nexus.example/npm/', snapshot: true, distTag: 'snapshot',
      dryRun: false})).toBe('pnpm -r publish --registry https://nexus.example/npm/ --tag snapshot --no-git-checks');
  });

  it('keeps every option of the publication in a dry run', () => {
    expect(publishCommand({...RELEASE, distTag: 'stable-18.4.x', dryRun: true})).toBe(
      'pnpm -r publish --registry https://registry.npmjs.org/ --access public --tag stable-18.4.x --no-git-checks'
        + ' --dry-run');
  });
});
