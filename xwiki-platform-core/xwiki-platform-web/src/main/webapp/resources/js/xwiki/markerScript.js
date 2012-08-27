/*
 * This script is a guard for making sure that the xwiki:dom:loading and xwiki:dom:loaded events are correctly sent
 * after all the deferred scripts have executed. This is needed because sometimes the browser fires DOMContentLoaded
 * before deferred scripts have actually executed, against the HTML5 specification. However, all browsers do respect
 * the order in which the scripts are declared. This script should always be the last script declared in the HEAD,
 * so that it will be executed when all the other scripts have trully executed.
 */
XWiki.lastScriptLoaded = true;
if (XWiki.failedInit) {
  XWiki.initialize();
}
