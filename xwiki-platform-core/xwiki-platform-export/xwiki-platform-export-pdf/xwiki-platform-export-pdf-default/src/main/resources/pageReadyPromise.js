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
new Promise((resolve, reject) => {
  const waitForPageReady = () => {
    require(['xwiki-page-ready'], function(pageReady) {
      pageReady.afterPageReady(() => {
        // Print to PDF only after all page ready callbacks were executed,
        // because the print preview is initialized as a page ready callback.
        pageReady.afterPageReady(resolve.bind(null, 'Page ready.'));
      });
    }, reject.bind(null, 'Failed to load the xwiki-page-ready module.'));
  };
  let retryCount = 0;
  const maybeWaitForPageReady = () => {
    if (typeof require === 'function') {
      waitForPageReady();
    } else {
      retryCount++;
      if (retryCount > 10) {
        reject('Timeout waiting for RequireJS to be available.');
      } else {
        setTimeout(maybeWaitForPageReady, 1000);
      }
    }
  };
  maybeWaitForPageReady();
});
