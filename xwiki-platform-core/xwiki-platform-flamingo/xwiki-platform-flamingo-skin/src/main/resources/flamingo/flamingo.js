require(['jquery'], function($) {
  'use strict';
  // See Bootstrap's @screen-sm-min
  var smallScreenMinWidth = 768;
  // Enable the default menu action (navigation link) if the screen is not too small (horizontal menu).
  $('.navbar-nav a.dropdown-split-left').each(function() {
    $(this).click(function(event) {
      if ($(window).width() >= smallScreenMinWidth) {
        // Prevent the menu from opening.
        event.stopPropagation();
        window.location = $(this).attr('href');
      }
    });
  });
});