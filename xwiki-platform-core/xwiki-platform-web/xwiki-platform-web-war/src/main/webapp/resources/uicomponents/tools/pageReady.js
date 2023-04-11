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

(function() {

  /**
   * Provides an API to execute code after the page is ready and to delay the ready state. A page is ready if it is loaded
   * and there aren't any pending HTTP requests or promises.
   */
  function definePageReady() {
    let resolvePageReady;
    let pageReadyPromise = new Promise((resolve) => {
      resolvePageReady = resolve;
    });

    let delayCount = 0, pendingDelays = new Map(), pageReadyTimeout;
    let delayPageReady = (promise, reason) => {
      clearTimeout(pageReadyTimeout);
      const delayId = delayCount++;
      pendingDelays.set(delayId, reason);
      promise.catch(() => {}).finally(() => {
        pendingDelays.delete(delayId);
        if (!pendingDelays.size) {
          // Mark the page as ready if there are no delays in the next 10ms.
          pageReadyTimeout = setTimeout(resolvePageReady, 10);
        }
      });
    };

    const getPendingDelays = () => new Map(pendingDelays);

    const afterPageReady = (callback) => {
      pageReadyPromise = pageReadyPromise.then(callback).catch(() => {});
    };

    afterPageReady(() => {
      // You cannot delay the page ready after the page is ready.
      delayPageReady = () => {};
    });

    const pageReady = {
      delayPageReady: function() {
        // We don't expose the internal delayPageReady because we want to modify it after the page is ready.
        return delayPageReady.apply(this, arguments);
      },
      getPendingDelays,
      afterPageReady
    };

    define('xwiki-page-ready', pageReady);

    return pageReady;
  };

  /**
   * Delays the page ready state as long as there are pending XMLHttpRequest, fetch calls or scripts being loaded,
   * marking the page as ready at the end.
   */
  function detectPageReady(pageReady) {
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
      }), 'window:load');
      return () => {
        window.removeEventListener('load', resolveWindowLoad);
      };
    };

    // Scripts are loaded only once and thus the load event is triggered only the first time a script is loaded, which is
    // why we need to keep track of the URLs that are successfuly loaded.
    const loadedURLs = new Set();

    /**
     * Resolve the given (string or URL) url as absolute URL or return the URL of the given request.
     *
     * @param urlOrRequest the url or request
     * @returns {string} the absolute URL (or undefined if it was not one of the expected types)
     */
    const getAbsoluteURL = function (urlOrRequest) {
      if (urlOrRequest instanceof Request) {
        return urlOrRequest.url;
      } else if (urlOrRequest instanceof URL) {
        return urlOrRequest.href;
      } else if (typeof urlOrRequest === "string") {
        return (new URL(urlOrRequest, window.location)).href;
      }
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
          const url = getAbsoluteURL(arguments[1]);
          pageReady.delayPageReady(new Promise((resolve, reject) => {
            this.addEventListener('load', () => {
              loadedURLs.add(url);
            });
            this.addEventListener('loadend', resolve);
          }), `xhr:${url}`);
        } catch (e) {
        }
        return originalOpen.apply(this, arguments);
      };
      // We cannot restore the original open because it may be overwritten by some other code.
      return () => {};
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
          const url = getAbsoluteURL(arguments[0]);
          pageReady.delayPageReady(fetchPromise.then(() => {
            loadedURLs.add(url);
          }), `fetch:${url}`);
        } catch (e) {
        }
        return fetchPromise;
      };
      // We cannot restore the original fetch because it may be overwritten by some other code.
      return () => {};
    };

    const interceptScriptLoad = function() {
      const scriptObserver = new MutationObserver(mutations => {
        mutations.forEach(mutation => {
          mutation.addedNodes.forEach(addedNode => {
            const src = addedNode.src;
            // Delay the page ready only if the script is not already loaded, otherwise the load event is not fired.
            if (addedNode.tagName === 'SCRIPT' && typeof src === 'string' && src !== ''
              && !loadedURLs.has(getAbsoluteURL(src)))
            {
              const url = getAbsoluteURL(src);
              pageReady.delayPageReady(new Promise((resolve, reject) => {
                addedNode.addEventListener('load', () => {
                  loadedURLs.add(url);
                  resolve();
                });
                addedNode.addEventListener('error', reject);
              }), `script:${url}`);
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

    pageReady.afterPageReady(() => {
      // Revert the interception once the page is ready.
      reverts.forEach(revert => revert());

      // Mark the page as beeing ready. This is useful for functional tests.
      document.documentElement.setAttribute('data-xwiki-page-ready', true);
    });
  };

  detectPageReady(definePageReady());

})();
