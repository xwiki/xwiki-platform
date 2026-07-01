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
// Execute the following code only in a Web Worker context.
if (!self.document) {
  (function() {
    // The fast-diff library is written in CommonJS format so it expects a `module` object to be available in the global
    // scope. Let's provide it.
    this.module = {};
    // The path used when the Velocity code is not evaluated (e.g. during unit tests).
    let fastDiffPath = "/webjars/fast-diff/diff.js";
    try {
      // This fails if the Velocity code is not evaluated.
      fastDiffPath = $jsontool.serialize(
        $services.webjars.url("org.webjars.npm:fast-diff", "diff.js")
      );
    } catch (e) {
      // Ignore.
    }
    importScripts(fastDiffPath);
    // The fast-diff library exports a single function that can be used to compare two strings.
    const diff = this.module.exports;

    this.onmessage = (event) => {
      this.postMessage(diff(event.data[0], event.data[1]));
    };
  }).call(self);
}
