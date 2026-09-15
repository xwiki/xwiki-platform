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

import fs from 'fs';
import path from 'path';
import {execFile} from 'child_process';
import {promisify} from 'util';

const execFileAsync = promisify(execFile);

/**
 * The npm executable installed next to the node executable by the frontend-maven-plugin, so that
 * the npm version used here is the one the build controls rather than whichever one happens to
 * come first on the PATH.
 *
 * @returns {string} the path to the npm executable, or "npm" when there is none next to node
 */
export function resolveNpmBinary() {
  const sibling = path.join(path.dirname(process.execPath), 'npm');

  return fs.existsSync(sibling) ? sibling : 'npm';
}

/**
 * Run the given tasks with a bounded number of them in flight at any time.
 *
 * @param {any[]} items the task inputs
 * @param {number} concurrency the maximum number of tasks running in parallel
 * @param {function(any): Promise<any>} task the task to run for each input
 * @returns {Promise<any[]>} the task results, in the order of the inputs
 */
export async function mapWithConcurrency(items, concurrency, task) {
  const results = new Array(items.length);
  let next = 0;

  const worker = async () => {
    while (next < items.length) {
      const current = next++;
      results[current] = await task(items[current]);
    }
  };

  await Promise.all(Array.from({length: Math.min(concurrency, items.length)}, worker));

  return results;
}

/**
 * Extract the npm error code from a failed npm invocation. With --json, npm reports the error as
 * JSON on its standard output, but it also keeps a human readable form on the standard error.
 *
 * @param {Error} error the error thrown by the runner
 * @returns {string|undefined} the npm error code, when there is one
 */
function npmErrorCode(error) {
  try {
    const parsed = JSON.parse(error.stdout);
    if (parsed?.error?.code) {
      return parsed.error.code;
    }
  } catch {
    // Not JSON, fall back to the standard error below.
  }

  return /\b(E\d{3})\b/.exec(error.stderr ?? '')?.[1];
}

/**
 * Read the dist-tags of the given packages from a registry.
 *
 * @param {string[]} names the package names
 * @param {{registry: string, npmBinary?: string, runner?: function, concurrency?: number}} options
 *   the registry to read from, and the injectable npm executable, runner and concurrency
 * @returns {Promise<{distTags: Object<string, Object<string, string>|null>,
 *   failures: {name: string, message: string}[]}>} the dist-tags of each package (null when the
 *   registry does not know the package) and the packages whose dist-tags could not be read
 */
export async function readDistTags(names, options) {
  const {registry, npmBinary = resolveNpmBinary(), runner = execFileAsync, concurrency = 8} = options;
  const distTags = {};
  const failures = [];

  await mapWithConcurrency(names, concurrency, async name => {
    const args = ['view', name, 'dist-tags', '--json', '--registry', registry];

    try {
      const {stdout} = await runner(npmBinary, args);
      // A package that exists but has no dist-tag at all prints nothing.
      distTags[name] = stdout.trim() === '' ? {} : JSON.parse(stdout);
    } catch (error) {
      if (npmErrorCode(error) === 'E404') {
        // The package has never been published: there is no "latest" to preserve.
        distTags[name] = null;
      } else {
        failures.push({name, message: error.message});
      }
    }
  });

  return {distTags, failures};
}

/**
 * Point a dist-tag of a package to a version.
 *
 * @param {{name: string, version: string, tag: string, registry: string, npmBinary?: string,
 *   runner?: function, attempts?: number, delays?: number[], sleep?: function}} options the
 *   dist-tag to write and the injectable npm executable, runner, retry policy and sleep function
 * @returns {Promise<void>} resolved once the dist-tag is written
 * @throws {Error} when every attempt failed
 */
export async function addDistTag(options) {
  const {
    name, version, tag, registry,
    npmBinary = resolveNpmBinary(),
    runner = execFileAsync,
    attempts = 3,
    delays = [1000, 4000],
    sleep = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds)),
  } = options;

  for (let attempt = 1; ; attempt++) {
    try {
      await runner(npmBinary, distTagArguments({name, version, tag, registry}));
      return;
    } catch (error) {
      if (attempt >= attempts) {
        throw error;
      }
      await sleep(delays[Math.min(attempt - 1, delays.length - 1)]);
    }
  }
}

/**
 * @param {{name: string, version: string, tag: string, registry: string}} target the dist-tag to
 *   write
 * @returns {string[]} the npm arguments writing that dist-tag
 */
export function distTagArguments({name, version, tag, registry}) {
  return ['dist-tag', 'add', `${name}@${version}`, tag, '--registry', registry];
}
