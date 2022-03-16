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
 * Provides an API to execute code after the page is ready and to delay the ready state. A page is ready if it is loaded
 * and there aren't any pending HTTP requests or promises.
 */
define('xwiki-page-ready', [], function() {
  let resolvePageReady;
  const pageReadyPromise = new Promise((resolve) => {
    resolvePageReady = resolve;
  });

  let delayCount = 0, pageReadyTimeout;
  const delayPageReady = (promise) => {
    clearTimeout(pageReadyTimeout);
    delayCount++;
    promise.catch(() => {}).finally(() => {
      delayCount--;
      if (delayCount <= 0) {
        // Mark the page as ready if there are no delays in the next 10ms.
        pageReadyTimeout = setTimeout(resolvePageReady, 10);
      }
    });
  };

  const onPageReady = (callback) => {
    pageReadyPromise.then(callback).catch(() => {});
  };

  return window.xwikiPageReady = {delayPageReady, onPageReady};
});

/**
 * Delays the page ready state as long as there are pending XMLHttpRequest, fetch calls or scripts being loaded.
 */
require(['xwiki-page-ready'], function(pageReady) {
  document.documentElement.setAttribute('data-xwiki-page-ready', false);

  // Make sure the page is ready only after the window is loaded. This also ensures that the page is always marked as
  // ready, even when there are no other delays (e.g. no additional HTTP requests).
  const interceptWindowLoad = function() {
    let resolveWindowLoad;
    pageReady.delayPageReady(new Promise((resolve, reject) => {
      if (document.readyState === 'complete') {
        // The window is already loaded.
        resolve();
      } else {
        // Wait for the window to be loaded.
        window.addEventListener('load', resolveWindowLoad = resolve);
      }
    }));
    return () => {
      window.removeEventListener('load', resolveWindowLoad);
    };
  };

  // HTTP requests made while the window is loading should delay the page ready.
  let interceptedOpen;
  const interceptXMLHttpRequest = function() {
    const originalOpen = window.XMLHttpRequest.prototype.open;
    if (originalOpen === interceptedOpen) {
      return;
    }
    interceptedOpen = window.XMLHttpRequest.prototype.open = function() {
      try {
        pageReady.delayPageReady(new Promise((resolve, reject) => {
          this.addEventListener('loadend', resolve);
        }));
      } catch (e) {
      }
      return originalOpen.apply(this, arguments);
    };
    return () => {
      window.XMLHttpRequest.prototype.open = originalOpen;
    };
  };

  let interceptedFetch;
  const interceptFetch = function() {
    const originalFetch = window.fetch;
    if (originalFetch === interceptedFetch) {
      return;
    }
    interceptedFetch = window.fetch = function() {
      const fetchPromise = originalFetch.apply(this, arguments);
      try {
        pageReady.delayPageReady(fetchPromise);
      } catch (e) {
      }
      return fetchPromise;
    };
    return () => {
      window.fetch = originalFetch;
    };
  };

  const interceptScriptLoad = function() {
    const scriptObserver = new MutationObserver(mutations => {
      mutations.forEach(mutation => {
        mutation.addedNodes.forEach(addedNode => {
          if (addedNode.tagName === 'SCRIPT' && typeof addedNode.src === 'string' && addedNode.src !== '') {
            pageReady.delayPageReady(new Promise((resolve, reject) => {
              addedNode.addEventListener('load', resolve);
              addedNode.addEventListener('error', reject);
            }));
          }
        });
      });
    });
    scriptObserver.observe(document.querySelector('head'), {childList: true});
    return () => {
      scriptObserver.disconnect();
    };
  };

  const reverts = [
    interceptWindowLoad(),
    interceptXMLHttpRequest(),
    interceptFetch(),
    interceptScriptLoad()
  ];

  pageReady.onPageReady(() => {
    // Revert the interception once the page is ready.
    reverts.forEach(revert => revert());

    // Mark the page as beeing ready. This is useful for functional tests.
    document.documentElement.setAttribute('data-xwiki-page-ready', true);
  });
});
