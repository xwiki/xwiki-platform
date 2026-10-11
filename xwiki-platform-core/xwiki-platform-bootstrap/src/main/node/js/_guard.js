/*
 * Bootstrap runtime guard.
 *
 * It performs two checks:
 *   1. jQuery must be loaded
 *   2. jQuery must be version 1.9.1 or higher
 *
 * This is likely not useful for XWiki but kept for backward compatibility.
 */
if (typeof jQuery === 'undefined') {
  throw new Error('Bootstrap\'s JavaScript requires jQuery')
}

+function ($) {
  'use strict';
  var version = $.fn.jquery.split(' ')[0].split('.')
  if ((version[0] < 2 && version[1] < 9) || (version[0] == 1 && version[1] == 9 && version[2] < 1)) {
    throw new Error('Bootstrap\'s JavaScript requires jQuery version 1.9.1 or higher')
  }
}(jQuery);
