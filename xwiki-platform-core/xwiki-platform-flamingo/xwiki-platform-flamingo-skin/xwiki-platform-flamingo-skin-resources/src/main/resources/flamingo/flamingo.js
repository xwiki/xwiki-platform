require(['jquery', 'bootstrap'], function($) {
  $(document).ready(function() {
  
    // Fix the bad location of the dropdown menu when the trigger is close to the end of the screen.
    // See: http://jira.xwiki.org/browse/XWIKI-12609
    $(document).on('shown.bs.dropdown', function () {
      $('[aria-expanded="true"]').each( function() {
        var toggle    = $(this);
        var menu      = toggle.next('.dropdown-menu');
        var menuWidth = menu.outerWidth();
        // if the right corner of the menu is after the end of the screen
        if (menu.offset().left + menuWidth > $(document.body).outerWidth()) {
          // we put that corner at the same place than the toggle's right corner
          var newLocation = toggle.offset().left + toggle.outerWidth() - menuWidth;
          // but don't put it negative, or the user will have to scroll to the left!
          if (newLocation < 0) {
            newLocation = 0;
          }
          menu.offset({'left': newLocation});
        }
      });
    });
    
    // Some variables used in the next 2 functions
    var globalSearch = $('#globalsearch');
    var globalSearchInput = globalSearch.find('input');
    var globalSearchButton = globalSearch.find('button');
    
    // Open the global search when the user click on the global search button
    globalSearchButton.click(function(event) {
      if (!globalSearch.hasClass('globalsearch-close') && globalSearchInput.val().length > 0) {
        return true;
      }
      globalSearch.removeClass('globalsearch-close');
      globalSearchInput.focus();
      return false;
    });
    
    // Close the global search when the focus is lost
    globalSearchInput.focusout(function() {
      // In order to let the main thread setting the focus to the new element, we execute the following code
      // in a callback.
      setTimeout( function () {
        // We close the global search only if the focus is on the search inpur or the search button
        if (document.activeElement !== globalSearchButton[0] && document.activeElement !== globalSearchInput[0]) {
          globalSearch.addClass('globalsearch-close');
        }
      }, 1);
    });
    
    // When the drawer is close, collapse sub items
    $(body).on('drawer.closed', function() {
      $('.drawer-menu-sub-item').removeClass('in').attr('aria-expanded', 'false');
    });
  
  });
});
