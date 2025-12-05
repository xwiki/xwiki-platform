if (typeof jQuery === 'undefined') {
  throw new Error('Bootstrap\'s JavaScript requires jQuery')
}

+function ($) {
  'use strict';
  var version = $.fn.jquery.split(' ')[0].split('.')
  if ((version[0] < 2 && version[1] < 9) || (version[0] == 1 && version[1] == 9 && version[2] < 1) || (version[0] > 3)) {
    throw new Error('Bootstrap\'s JavaScript requires jQuery version 1.9.1 or higher, but lower than version 4')
  }
}(jQuery);

import('./transition.js');
import('./alert.js');
import('./button.js');
import('./carousel.js');
import('./collapse.js');
import('./dropdown.js');
import('./modal.js');
import('./tooltip.js');
import('./popover.js');
import('./scrollspy.js');
import('./tab.js');
import('./affix.js');