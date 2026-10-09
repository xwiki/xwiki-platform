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
import {addDistTag, distTagArguments, mapWithConcurrency, readDistTags} from './registry.js';

/**
 * @param {Object} stdout the dist-tags each package name answers with
 * @returns {function} a runner behaving like "npm view <name> dist-tags --json"
 */
function viewRunner(stdout) {
  return vi.fn(async (npmBinary, args) => ({stdout: stdout[args[1]]}));
}

/**
 * @param {string} code the npm error code to fail with
 * @returns {Error} the error the npm CLI produces for that code
 */
function npmError(code) {
  const error = new Error(`Command failed with ${code}`);
  error.stdout = JSON.stringify({error: {code, summary: 'not found'}});
  error.stderr = `npm error code ${code}`;

  return error;
}

describe('readDistTags', () => {
  it('reads the dist-tags of each package from the given registry', async () => {
    const runner = viewRunner({a: '{"latest":"18.4.5","rc":"18.8.0-rc-1"}', b: '{"latest":"18.7.0"}'});

    const result = await readDistTags(['a', 'b'], {registry: 'https://registry', npmBinary: 'npm', runner});

    expect(result).toEqual({
      distTags: {a: {latest: '18.4.5', rc: '18.8.0-rc-1'}, b: {latest: '18.7.0'}},
      failures: [],
    });
    expect(runner).toHaveBeenCalledWith('npm',
      ['view', 'a', 'dist-tags', '--json', '--registry', 'https://registry']);
  });

  it('reports a package unknown to the registry as having no dist-tag', async () => {
    const runner = vi.fn(async () => {
      throw npmError('E404');
    });

    const result = await readDistTags(['new'], {registry: 'https://registry', npmBinary: 'npm', runner});

    expect(result).toEqual({distTags: {new: null}, failures: []});
  });

  it('detects the npm error code from the standard error too', async () => {
    const runner = vi.fn(async () => {
      const error = new Error('failed');
      error.stdout = 'not json';
      error.stderr = 'npm error code E404\nnpm error 404 Not Found';
      throw error;
    });

    const result = await readDistTags(['new'], {registry: 'https://registry', npmBinary: 'npm', runner});

    expect(result.distTags).toEqual({new: null});
  });

  it('reports any other failure instead of assuming there is no dist-tag', async () => {
    const runner = vi.fn(async (npmBinary, args) => {
      if (args[1] === 'broken') {
        throw npmError('E500');
      }
      return {stdout: '{"latest":"18.4.5"}'};
    });

    const result = await readDistTags(['fine', 'broken'], {
      registry: 'https://registry',
      npmBinary: 'npm',
      runner,
    });

    expect(result.distTags).toEqual({fine: {latest: '18.4.5'}});
    expect(result.failures).toEqual([{name: 'broken', message: 'Command failed with E500'}]);
  });

  it('treats a package published without any dist-tag as having none', async () => {
    const runner = viewRunner({empty: '\n'});

    const result = await readDistTags(['empty'], {registry: 'https://registry', npmBinary: 'npm', runner});

    expect(result.distTags).toEqual({empty: {}});
  });
});

describe('addDistTag', () => {
  const target = {name: '@xwiki/platform-api', version: '18.7.0', tag: 'latest', registry: 'https://registry'};

  it('points the dist-tag to the version', async () => {
    const runner = vi.fn(async () => ({stdout: ''}));

    await addDistTag({...target, npmBinary: 'npm', runner});

    expect(runner).toHaveBeenCalledExactlyOnceWith('npm',
      ['dist-tag', 'add', '@xwiki/platform-api@18.7.0', 'latest', '--registry', 'https://registry']);
  });

  it('retries a failing write', async () => {
    const runner = vi.fn()
      .mockRejectedValueOnce(new Error('ETIMEDOUT'))
      .mockResolvedValueOnce({stdout: ''});
    const sleep = vi.fn();

    await addDistTag({...target, npmBinary: 'npm', runner, sleep});

    expect(runner).toHaveBeenCalledTimes(2);
    expect(sleep).toHaveBeenCalledExactlyOnceWith(1000);
  });

  it('gives up after the last attempt and surfaces the error', async () => {
    const runner = vi.fn().mockRejectedValue(new Error('ETIMEDOUT'));
    const sleep = vi.fn();

    await expect(addDistTag({...target, npmBinary: 'npm', runner, sleep})).rejects.toThrow('ETIMEDOUT');
    expect(runner).toHaveBeenCalledTimes(3);
    expect(sleep.mock.calls).toEqual([[1000], [4000]]);
  });
});

describe('distTagArguments', () => {
  it('builds a command that can be copied into a terminal to repair a dist-tag', () => {
    expect(distTagArguments({
      name: '@xwiki/platform-api',
      version: '18.7.0',
      tag: 'latest',
      registry: 'https://registry.npmjs.org',
    }).join(' ')).toBe(
      'dist-tag add @xwiki/platform-api@18.7.0 latest --registry https://registry.npmjs.org');
  });
});

describe('mapWithConcurrency', () => {
  it('runs every task and keeps the order of the inputs', async () => {
    const result = await mapWithConcurrency([1, 2, 3, 4, 5], 2, async item => item * 2);

    expect(result).toEqual([2, 4, 6, 8, 10]);
  });

  it('never runs more tasks in parallel than allowed', async () => {
    let running = 0;
    let peak = 0;

    await mapWithConcurrency([1, 2, 3, 4, 5, 6], 2, async () => {
      peak = Math.max(peak, ++running);
      await Promise.resolve();
      running--;
    });

    expect(peak).toBe(2);
  });

  it('does nothing when there is nothing to do', async () => {
    const task = vi.fn();

    expect(await mapWithConcurrency([], 8, task)).toEqual([]);
    expect(task).not.toHaveBeenCalled();
  });
});
